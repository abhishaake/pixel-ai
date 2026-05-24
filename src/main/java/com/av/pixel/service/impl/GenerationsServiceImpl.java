package com.av.pixel.service.impl;

import com.av.pixel.cache.RLock;
import com.av.pixel.cache.Cache;
import com.av.pixel.client.GoEnhanceClient;
import com.av.pixel.client.IdeogramClient;
import com.av.pixel.dao.VideoEffectConfig;
import com.av.pixel.dao.VideoEffectJob;
import com.av.pixel.dto.VideoEffectConfigDTO;
import com.av.pixel.enums.GoEnhanceResolutionEnum;
import com.av.pixel.enums.VideoEffectJobStatusEnum;
import com.av.pixel.repository.VideoEffectConfigRepository;
import com.av.pixel.repository.VideoEffectJobRepository;
import com.av.pixel.response.goenhance.GoEnhanceEffectListResponse;
import com.av.pixel.request.VideoEffectRequest;
import com.av.pixel.response.goenhance.GoEnhanceGenerateResponse;
import com.av.pixel.response.goenhance.GoEnhanceJobResponse;
import com.av.pixel.dao.Generations;
import com.av.pixel.dao.ImageFlag;
import com.av.pixel.dao.ModelConfig;
import com.av.pixel.dao.User;
import com.av.pixel.dto.GenerationsDTO;
import com.av.pixel.dto.ModelPricingDTO;
import com.av.pixel.dto.UserCreditDTO;
import com.av.pixel.dto.UserDTO;
import com.av.pixel.enums.IdeogramModelEnum;
import com.av.pixel.enums.ImageActionEnum;
import com.av.pixel.enums.ImageCompressionConfig;
import com.av.pixel.enums.ImagePrivacyEnum;
import com.av.pixel.enums.ImageRenderOptionEnum;
import com.av.pixel.enums.ImageStyleEnum;
import com.av.pixel.enums.OrderTypeEnum;
import com.av.pixel.enums.PixelModelEnum;
import com.av.pixel.exception.Error;
import com.av.pixel.exception.IdeogramException;
import com.av.pixel.exception.IdeogramServerException;
import com.av.pixel.exception.IdeogramUnprocessableEntityException;
import com.av.pixel.helper.AsyncUtil;
import com.av.pixel.helper.DateUtil;
import com.av.pixel.helper.GenerationHelper;
import com.av.pixel.helper.TransformUtil;
import com.av.pixel.helper.UserCreditHelper;
import com.av.pixel.helper.Validator;
import com.av.pixel.mapper.GenerationsMap;
import com.av.pixel.mapper.ModelConfigMap;
import com.av.pixel.mapper.UserCreditMap;
import com.av.pixel.mapper.ideogram.ImageMap;
import com.av.pixel.repository.ImageFlagRepository;
import com.av.pixel.repository.ModelConfigRepository;
import com.av.pixel.request.GenerateRequest;
import com.av.pixel.request.GenerationsFilterRequest;
import com.av.pixel.request.ImageActionRequest;
import com.av.pixel.request.ImagePricingRequest;
import com.av.pixel.request.ImageReportRequest;
import com.av.pixel.request.SortByRequest;
import com.av.pixel.request.ideogram.ImageRequest;
import com.av.pixel.response.GenerationsFilterResponse;
import com.av.pixel.response.ImagePricingResponse;
import com.av.pixel.response.ModelConfigResponse;
import com.av.pixel.response.ideogram.ImageResponse;
import com.av.pixel.service.AdminConfigService;
import com.av.pixel.service.GenerationsService;
import com.av.pixel.service.ImageCompressionService;
import com.av.pixel.service.GenerationActionService;
import com.av.pixel.service.S3Service;
import com.av.pixel.service.SesEmailService;
import com.av.pixel.service.UserCreditService;
import com.av.pixel.service.UserService;
import com.av.pixel.service.VideoThumbnailService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
@AllArgsConstructor
public class GenerationsServiceImpl implements GenerationsService {

    private final MongoTemplate mongoTemplate;
    private final UserCreditService userCreditService;
    private final ModelConfigRepository modelConfigRepository;
    private final IdeogramClient ideogramClient;
    private final GoEnhanceClient goEnhanceClient;
    private final VideoEffectJobRepository videoEffectJobRepository;
    private final VideoEffectConfigRepository videoEffectConfigRepository;
    private final GenerationHelper generationHelper;
    private final GenerationActionService generationActionService;
    private final RLock locker;
    private final UserService userService;
    private final AdminConfigService adminConfigService;
    private final S3Service s3Service;
    private final ImageCompressionService imageCompressionService;
    private final ImageFlagRepository imageFlagRepository;
    private final EmailService emailService;
    private final SesEmailService sesEmailService;
    private final AsyncUtil asyncUtil;
    private final BlockUserService blockUserService;
    private final UserCreditHelper userCreditHelper;
    private final VideoThumbnailService videoThumbnailService;

    private static final String IMAGE_UNSAFE_LOGO = "https://av-pixel.s3.ap-south-1.amazonaws.com/image_not_safe_logo.jpeg";

    @Override
    public GenerationsDTO generate (UserDTO userDTO, GenerateRequest generateRequest, MultipartFile file) {
        log.info("generate img req {} from {}", generateRequest.getPrompt(), userDTO.getCode());
        Validator.validateGenerateRequest(generateRequest);

        String key = "generation_" + userDTO.getCode();
        boolean locked = locker.tryLock(key, 10);

        if (!locked) {
            throw new Error("1 Generation already in progress, Please wait..");
        }

        AtomicInteger availableCredits = new AtomicInteger(0);

        try {
            // Execute credit check and cost calculation concurrently
            CompletableFuture<UserCreditDTO> creditFuture = asyncUtil.executeAsync(() -> {
                UserCreditDTO userCreditDTO = userCreditService.getUserCredit(userDTO.getCode());
                if (Objects.isNull(userCreditDTO)) {
                    userCreditDTO = UserCreditMap.userCreditDTO(userCreditService.createNewUserCredit(userDTO.getCode()));
                }
                return userCreditDTO;
            });

            CompletableFuture<Integer> costFuture = asyncUtil.executeAsync(() -> getCost(generateRequest));

            // Wait for both operations to complete
            UserCreditDTO userCreditDTO = creditFuture.get();
            Integer imageGenerationCost = costFuture.get();

            availableCredits.set(userCreditDTO.getAvailable());
            if (availableCredits.get() < imageGenerationCost) {
                throw new Error(HttpStatus.PAYMENT_REQUIRED, "Not enough credits");
            }

            ImageRequest imageRequest = ImageMap.validateAndGetImageRequest(generateRequest,file);

            // Execute image generation asynchronously
            CompletableFuture<List<ImageResponse>> imageGenerationFuture = asyncUtil.executeAsync(() -> 
                generateImage(imageRequest, userDTO.getCode(), availableCredits.get()));

            List<ImageResponse> imageResponses = imageGenerationFuture.get();

            if (Objects.isNull(imageResponses)) {
                throw new Error("Some error occurred, please try again");
            }

            final String characterRefImageUrl = safeUploadRefImage(userDTO.getCode(), file);

            // Execute database operations concurrently
            CompletableFuture<Generations> saveGenerationFuture = asyncUtil.executeAsync(() -> 
                generationHelper.saveUserGeneration(userDTO.getCode(), generateRequest, imageRequest, imageResponses, imageGenerationCost, characterRefImageUrl));

            Generations generations = saveGenerationFuture.get();


            // Execute credit debit asynchronously (fire and forget)
            asyncUtil.executeAsync(() -> {
                userCreditService.debitUserCredit(userDTO.getCode(), imageGenerationCost, OrderTypeEnum.IMAGE_GENERATION, "SERVER", generations.getId().toString());
                return null;
            });
            GenerationsDTO res = GenerationsMap.toGenerationsDTO(generations);
            assert res != null;
            res.setUserName(userDTO.getFirstName())
                .setUserImgUrl(userDTO.getImageUrl());

            locker.unlock(key);
            return res;
        }
        catch (Error e) {
            Thread.currentThread().interrupt();
            locker.unlock(key);
            throw e;
        }
        catch (IdeogramUnprocessableEntityException e) {
            Thread.currentThread().interrupt();
            locker.unlock(key);
            throw new Error(e.getError());
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            locker.unlock(key);
            log.error("Thread interrupted while waiting for async operations", e);
            throw new RuntimeException("Operation interrupted", e);
        }
        catch (ExecutionException e) {
            locker.unlock(key);
            log.error("Error executing async operations", e);
            if (e.getCause() instanceof IdeogramUnprocessableEntityException ce) {
                throw ce;
            } else if (e.getCause() instanceof Error ce) {
                throw ce;
            } else if (e.getCause() instanceof IdeogramServerException ce) {
                throw ce;
            }
            throw new RuntimeException("Error processing request", e);
        }
        catch (Exception e) {
            Thread.currentThread().interrupt();
            locker.unlock(key);
            throw e;
        }
    }

    private void throwCustomUnprocessableEntityException(Integer credits, IdeogramUnprocessableEntityException e) {
        if (credits == null || credits <= userCreditHelper.getDefaultUserCredit()) {
            throw new Error("The images for the given prompt may not be available on free version");
        }
    }

    String safeUploadRefImage(String userCode, MultipartFile file) {
        try{
            if ( file == null ){
                return null;
            }
            String fileName = getFileName(userCode + "_ref", DateUtil.currentTimeMillis());
            return s3Service.uploadFile(fileName,file.getBytes());
        }catch (Exception e) {
            log.error("Error uploading file", e);
            return null;
        }
    }



    private List<ImageResponse> generateImage (ImageRequest imageRequest, String userCode, int availableCredits) {
        List<ImageResponse> res = null;
        try {
            if (adminConfigService.isIdeogramClientDisabled(userCode)) {
                return generationHelper.generateImages(imageRequest);
            }
            if(imageRequest.getModel() == IdeogramModelEnum.V_3_QUALITY || imageRequest.getModel() == IdeogramModelEnum.V_3_TURBO){
                res = ideogramClient.generateImagesV2(imageRequest);
            }else {
                res = ideogramClient.generateImages(imageRequest);
            }
        } catch (IdeogramUnprocessableEntityException e) {
            throwCustomUnprocessableEntityException(availableCredits, e);
            throw new Error(e.getError());
        }
        catch (IdeogramException e) {
            return null;
        } catch (Exception e) {
            return null;
        }
        checkForSafeImages(imageRequest, res, userCode);
        try {
            uploadToS3Async(res, userCode);
            return res;
        }
        catch (Exception e){
            log.error("uploading error", e);
            return res;
        }
    }

    private void checkForSafeImages(ImageRequest imageRequest, List<ImageResponse> res, String userCode) {
        if (CollectionUtils.isEmpty(res)) {
            return;
        }
        int size = res.size();
        int unsafeImages = 0;
        try {
            res.sort(Comparator.comparing(ImageResponse::getIsImageSafe).reversed());

            for (int i = 0; i < size; i++) {
                if (Boolean.FALSE.equals(res.get(i).getIsImageSafe())) {
                    unsafeImages++;
                    res.get(i).setUrl(IMAGE_UNSAFE_LOGO);
                    res.get(i).setThumbnailUrl(IMAGE_UNSAFE_LOGO);
                }
            }
        } catch (Exception e) {
            log.error("[CRITICAL] ", e);
        }
        if (unsafeImages > 0) {
            String body = "[CRITICAL] ideogram exception " + " \n\n requestBody: " + TransformUtil.toJson(imageRequest)
                    + "\n \n error : Found " + unsafeImages + " unsafe images "
                    + "\n \n user Code : " + userCode;
            sesEmailService.sendErrorMail(body);
        }
        if (unsafeImages == size) {
            throw new IdeogramUnprocessableEntityException();
        }
    }

    private void uploadToS3 (List<ImageResponse> res, String userCode) {
        if (CollectionUtils.isEmpty(res)){
            return;
        }
        int idx = 0;
        long epoch = DateUtil.currentTimeMillis();
        for(ImageResponse imageResponse : res) {
            try {
                if (imageResponse.getIsImageSafe()) {
                    uploadToS3(imageResponse, userCode, epoch, idx);
                    idx++;
                }
            }
            catch(Exception e){
                log.error("uploadToS3 error", e);
            }
        }
    }

    private void uploadToS3Async (List<ImageResponse> res, String userCode) {
        if (CollectionUtils.isEmpty(res)){
            return;
        }
        
        long epoch = DateUtil.currentTimeMillis();
        List<CompletableFuture<Void>> uploadFutures = new ArrayList<>();
        
        for (int i = 0; i < res.size(); i++) {
            final ImageResponse imageResponse = res.get(i);
            final int idx = i;
            
            if (imageResponse.getIsImageSafe()) {
                CompletableFuture<Void> uploadFuture = asyncUtil.executeAsync(() -> {
                    try {
                        uploadToS3(imageResponse, userCode, epoch, idx);
                    } catch (Exception e) {
                        log.error("Async uploadToS3 error for image {}", idx, e);
                    }
                    return null;
                });
                uploadFutures.add(uploadFuture);
            }
        }
        
        // Wait for all uploads to complete
        try {
            CompletableFuture.allOf(uploadFutures.toArray(new CompletableFuture[0])).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Thread interrupted while waiting for S3 uploads", e);
        } catch (ExecutionException e) {
            log.error("Error during S3 uploads", e);
        }
    }

    public ImageResponse uploadToS3 (ImageResponse imageResponse, String userCode, Long epoch, int idx) {
        HttpResponse<byte[]> imageRes = s3Service.downloadImage(imageResponse.getUrl());
        String fileName = getFileName(userCode, epoch + idx);
        String extension = s3Service.getImageExtensionName(imageRes);
        String url = s3Service.uploadToS3(imageRes.body(), fileName + extension);
        imageResponse.setUrl(url);

        double imageSize = imageCompressionService.getImageSize(imageRes.body());
        if (imageCompressionService.isCompressionRequired(imageSize)) {
            ImageCompressionConfig config = imageCompressionService.getRequiredCompression(imageSize);
            if (Objects.isNull(config)) {
                imageResponse.setThumbnailUrl(url);
            } else {
                byte[] compressedImage = imageCompressionService.getCompressedImage(imageRes.body(), config);
                imageResponse.setThumbnailUrl(s3Service.uploadToS3(compressedImage, fileName + "_thumbnail"+ extension));
            }
        } else {
            imageResponse.setThumbnailUrl(url);
        }
        return imageResponse;
    }


    private String getFileName (String userCode, long epoch) {
        return userCode + "_" + epoch;
    }

    @Override
    public GenerationsFilterResponse filterImages (UserDTO userDTO, GenerationsFilterRequest generationsFilterRequest, boolean includeVideoEffects) {
        String userCode = (Objects.nonNull(userDTO) && StringUtils.isNotEmpty(userDTO.getCode())) ? userDTO.getCode() : null;

        Validator.validateFilterImageRequest(generationsFilterRequest, "");
        boolean isSelfProfile = StringUtils.isNotEmpty(userCode) && !CollectionUtils.isEmpty(generationsFilterRequest.getUserCodes())
                && generationsFilterRequest.getUserCodes().contains(userCode);

        ImagePrivacyEnum privacyEnum = ImagePrivacyEnum.getEnumByName(generationsFilterRequest.getPrivacy());

        if (isSelfProfile && ImagePrivacyEnum.DEFAULT.equals(privacyEnum)) {
            privacyEnum = ImagePrivacyEnum.BOTH;
        } else if (StringUtils.isNotEmpty(userCode) && List.of("P108", "P125", "P109").contains(userCode)) {
            privacyEnum = ImagePrivacyEnum.BOTH;
        }

       if (!CollectionUtils.isEmpty(generationsFilterRequest.getStyles())) {
           generationsFilterRequest.setStyles(ImageStyleEnum.getEnumsForFilter(generationsFilterRequest.getStyles()));
       }

        try {
            List<String> blockedUsers = blockUserService.getBlockedUsers(userCode);
            
            // Execute database query to get generations page
            Page<Generations> generationsPage = findByFilters(generationsFilterRequest.getUserCodes(),
                    generationsFilterRequest.getCategories(),
                    generationsFilterRequest.getStyles(),
                    privacyEnum.getPrivateImage(),
                    generationsFilterRequest.getSort(),
                    blockedUsers,
                    includeVideoEffects,
                    PageRequest.of(generationsFilterRequest.getPage(), generationsFilterRequest.getSize()));

            long totalCount = generationsPage.getTotalElements();
            
            // Prepare data for concurrent execution
            final List<String> genIds;
            final List<String> userCodes;
            
            if (!CollectionUtils.isEmpty(generationsPage.getContent())) {
                genIds = generationsPage.getContent().stream().map(g -> g.getId().toString()).toList();
                userCodes = generationsPage.getContent().stream().map(Generations::getUserCode).toList();
            } else {
                genIds = new ArrayList<>();
                userCodes = new ArrayList<>();
            }

            // Execute independent operations concurrently
            CompletableFuture<TreeSet<String>> likedGenerationsFuture = CompletableFuture.completedFuture(null);
            CompletableFuture<Map<String, User>> userMapFuture = CompletableFuture.completedFuture(null);

            if (StringUtils.isNotEmpty(userCode) && !CollectionUtils.isEmpty(genIds)) {
                likedGenerationsFuture = asyncUtil.executeAsync(() -> 
                    generationActionService.getLikedGenerationsByUserCode(userCode, genIds));
            }

            if (!CollectionUtils.isEmpty(userCodes)) {
                userMapFuture = asyncUtil.executeAsync(() -> 
                    userService.getUserCodeVsUserMap(userCodes));
            }

            // Wait for all async operations to complete
            TreeSet<String> likedGenerations = likedGenerationsFuture.get();
            Map<String, User> userMap = userMapFuture.get();

            return new GenerationsFilterResponse(
                GenerationsMap.toList(generationsPage.getContent(), likedGenerations, userMap),
                totalCount, 
                generationsFilterRequest.getPage(), 
                generationsPage.getNumberOfElements()
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Thread interrupted while waiting for async operations", e);
            throw new RuntimeException("Operation interrupted", e);
        } catch (ExecutionException e) {
            log.error("Error executing async operations", e);
            throw new RuntimeException("Error processing request", e);
        }
    }


    public Page<Generations> findByFilters (List<String> userCodes,
                                            List<String> categories,
                                            List<String> styles,
                                            Boolean privacy,
                                            SortByRequest sortByRequest,
                                            List<String> blockedUsers,
                                            boolean includeVideoEffects,
                                            Pageable pageable) {

        List<Criteria> criteriaList = new ArrayList<>();

        if (userCodes != null && !userCodes.isEmpty()) {
            criteriaList.add(Criteria.where("userCode").in(userCodes));
        }

        if (categories != null && !categories.isEmpty()) {
            criteriaList.add(Criteria.where("category").in(categories));
        }

        if (styles != null && !styles.isEmpty()) {
            criteriaList.add(Criteria.where("style").in(styles));
        }

        if (privacy != null) {
            criteriaList.add(Criteria.where("privateImage").is(privacy));
        }

        if (!CollectionUtils.isEmpty(blockedUsers)) {
            criteriaList.add(Criteria.where("userCode").not().in(blockedUsers));
        }

        if (!includeVideoEffects) {
            criteriaList.add(Criteria.where("videoEffect").ne(true));
        }

        Query query = new Query();

        if (Objects.nonNull(sortByRequest) && StringUtils.isNotEmpty(sortByRequest.getSortBy())
                && Objects.nonNull(sortByRequest.getSortDir())) {
            query.with(Sort.by(sortByRequest.getSortDir(), sortByRequest.getSortBy()));
        } else {
            query.with(Sort.by(Sort.Direction.DESC, "views", "likes"));
        }

        if (!criteriaList.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criteriaList.toArray(new Criteria[0])));
        }

        long total = mongoTemplate.count(query, Generations.class);

        query.with(pageable);

        List<Generations> results = mongoTemplate.find(query, Generations.class);

        return new PageImpl<>(results, pageable, total);
    }

    private Integer getCost (GenerateRequest generateRequest) {
        ImagePricingRequest imagePricingRequest = new ImagePricingRequest().setModel(generateRequest.getModel())
                .setNoOfImages(generateRequest.getNoOfImages())
                .setSeed(generateRequest.getSeed())
                .setPrivateImage(generateRequest.getPrivateImage())
                .setNegativePrompt(generateRequest.getNegativePrompt())
                .setHaveCharacterFile(generateRequest.getHaveCharacterFile())
                .setRenderOption(generateRequest.getRenderOption());

        ImagePricingResponse imagePricingResponse = getPricing(imagePricingRequest);
        return imagePricingResponse.getFinalCost();
    }


    @Override
    public ImagePricingResponse getPricing (ImagePricingRequest imagePricingRequest) {

        Validator.validateModelPricingRequest(imagePricingRequest);
        ImageRenderOptionEnum renderOptionEnum = ImageRenderOptionEnum.getEnumByName(imagePricingRequest.getRenderOption());
        IdeogramModelEnum pixelModelEnum = PixelModelEnum.getIdeogramModelByNameAndRenderOption(imagePricingRequest.getModel(), renderOptionEnum);

        if (Objects.isNull(pixelModelEnum)) {
            throw new Error("Please select valid model");
        }

        String model = pixelModelEnum.name();

        ModelPricingDTO modelPricingDTO = Cache.getModelPricingMap().get(model);

        if (Objects.isNull(modelPricingDTO)) {
            throw new Error("modelPricing not found");
        }

        boolean isSeed = Objects.nonNull(imagePricingRequest.getSeed());
        boolean isCharacter = Objects.requireNonNullElse(imagePricingRequest.getHaveCharacterFile(), false);

        Integer finalCost = modelPricingDTO.getFinalCost(imagePricingRequest.getNoOfImages(),
                imagePricingRequest.isPrivateImage(), isSeed, StringUtils.isNotEmpty(imagePricingRequest.getNegativePrompt()),isCharacter);

        return new ImagePricingResponse()
                .setFinalCost(finalCost);
    }

    @Override
    public ModelConfigResponse getModelConfigs () {
        List<ModelConfig> modelConfigs = modelConfigRepository.findAllByDeletedFalseOrderByOrderDesc();

        if (CollectionUtils.isEmpty(modelConfigs)) {
            throw new Error("no model config found");
        }

        return new ModelConfigResponse()
                .setModels(ModelConfigMap.toList(modelConfigs));
    }

    @Override
    public String performAction (UserDTO userDTO, ImageActionRequest imageActionRequest) {
        String key = "action_" + imageActionRequest.getGenerationId();
        boolean locked = locker.tryLock(key, 10);

        if (!locked) {
            return "success";
        }
        String res = "success";
        try {
            if (ImageActionEnum.LIKE.equals(imageActionRequest.getAction())) {
                res = generationActionService.likeGeneration(userDTO.getCode(), imageActionRequest.getGenerationId());
            } else if (ImageActionEnum.DISLIKE.equals(imageActionRequest.getAction())) {
                res = generationActionService.disLikeGeneration(userDTO.getCode(), imageActionRequest.getGenerationId());
            }
            return "success";
        } catch (Exception e) {
            Thread.currentThread().interrupt();
        } finally {
            locker.unlock(key);
        }
        return res;
    }

    @Override
    public String addView (UserDTO userDTO, ImageActionRequest imageActionRequest) {
        if (ImageActionEnum.VIEW.equals(imageActionRequest.getAction())) {
            return generationActionService.addView(imageActionRequest.getGenerationId());
        }
        return "success";
    }

    private static final int VIDEO_EFFECT_COST = 200;

    @Override
    public GenerationsDTO generateVideoEffect(UserDTO userDTO, VideoEffectRequest request, MultipartFile file) {
        log.info("generateVideoEffect effect={} from {}", request.getEffect(), userDTO.getCode());

        if (request.getEffect() == null || request.getEffect().isBlank()) {
            throw new Error(HttpStatus.BAD_REQUEST, "Effect is required");
        }

        String effectId = request.getEffect();
        String key = "generation_" + userDTO.getCode();
        boolean locked = locker.tryLock(key, 10);

        if (!locked) {
            throw new Error("1 Generation already in progress, Please wait..");
        }

        try {
            UserCreditDTO userCreditDTO = userCreditService.getUserCredit(userDTO.getCode());
            if (Objects.isNull(userCreditDTO)) {
                userCreditDTO = UserCreditMap.userCreditDTO(userCreditService.createNewUserCredit(userDTO.getCode()));
            }

            if (userCreditDTO.getAvailable() < VIDEO_EFFECT_COST) {
                throw new Error(HttpStatus.PAYMENT_REQUIRED, "Not enough credits");
            }

            String referenceImageUrl = safeUploadRefImage(userDTO.getCode(), file);
            if (referenceImageUrl == null) {
                throw new Error(HttpStatus.BAD_REQUEST, "Reference image is required");
            }

            String resolution = GoEnhanceResolutionEnum.fromValue(request.getResolution()).getValue();
            GoEnhanceGenerateResponse generateResponse = goEnhanceClient.generateVideoEffect(effectId, referenceImageUrl, resolution);
            if (generateResponse == null || generateResponse.getImgUuid() == null) {
                throw new Error("Failed to submit video effect job, please try again");
            }

            VideoEffectJob job = videoEffectJobRepository.save(new VideoEffectJob()
                    .setImgUuid(generateResponse.getImgUuid())
                    .setUserCode(userDTO.getCode())
                    .setEffect(effectId)
                    .setResolution(resolution)
                    .setReferenceImageUrl(referenceImageUrl)
                    .setPrivateImage(request.getPrivateImage())
                    .setStatus(VideoEffectJobStatusEnum.PENDING));

            String videoUrl = pollForVideoUrl(generateResponse.getImgUuid());

            locker.unlock(key);

            if (videoUrl != null) {
                Generations generations = completeVideoEffectJob(job, videoUrl);
                GenerationsDTO res = GenerationsMap.toGenerationsDTO(generations);
                assert res != null;
                return res.setUserName(userDTO.getFirstName()).setUserImgUrl(userDTO.getImageUrl());
            }

            return new GenerationsDTO().setMessage("We will inform you when your video is ready");
        } catch (Error e) {
            locker.unlock(key);
            throw e;
        } catch (Exception e) {
            locker.unlock(key);
            log.error("generateVideoEffect error", e);
            throw new Error("Some error occurred, please try again");
        }
    }

    private Generations completeVideoEffectJob(VideoEffectJob job, String goEnhanceVideoUrl) {
        // Download the video once — reuse bytes for both the S3 upload and thumbnail extraction
        java.net.http.HttpResponse<byte[]> videoResponse = s3Service.downloadImage(goEnhanceVideoUrl);
        byte[] videoBytes = videoResponse.body();
        String baseFileName = getFileName(job.getUserCode(), DateUtil.currentTimeMillis());
        String extension = s3Service.getImageExtensionName(videoResponse);

        String s3VideoUrl = s3Service.uploadToS3(videoBytes, baseFileName + extension);
        String thumbnailUrl = extractAndUploadVideoThumbnail(videoBytes, baseFileName);

        Generations generations = generationHelper.saveVideoEffectGeneration(
                job.getUserCode(), job.getEffect(), s3VideoUrl, thumbnailUrl,
                job.getReferenceImageUrl(), job.getResolution(), Boolean.TRUE.equals(job.getPrivateImage()));

        asyncUtil.executeAsync(() -> {
            userCreditService.debitUserCredit(job.getUserCode(), VIDEO_EFFECT_COST,
                    OrderTypeEnum.VIDEO_EFFECT, "SERVER", generations.getId().toString());
            return null;
        });

        job.setStatus(VideoEffectJobStatusEnum.COMPLETED);
        videoEffectJobRepository.save(job);

        return generations;
    }

    /**
     * Extracts a JPEG thumbnail from the video bytes using FFmpeg and uploads it to S3.
     * Returns {@code null} if extraction fails — callers fall back to the video URL itself.
     */
    private String extractAndUploadVideoThumbnail(byte[] videoBytes, String baseFileName) {
        try {
            byte[] thumbnailBytes = videoThumbnailService.extractThumbnail(videoBytes);
            if (thumbnailBytes == null) {
                log.warn("[VideoThumbnail] extraction returned null for file={}, thumbnail will be null", baseFileName);
                return null;
            }
            return s3Service.uploadToS3(thumbnailBytes, baseFileName + "_thumbnail.jpg");
        } catch (Exception e) {
            log.error("[VideoThumbnail] upload error for file={}", baseFileName, e);
            return null;
        }
    }

    @Override
    public void processPendingVideoEffectJobs() {
        List<VideoEffectJob> pendingJobs = videoEffectJobRepository.findAllByStatusAndDeletedFalse(VideoEffectJobStatusEnum.PENDING);
        if (CollectionUtils.isEmpty(pendingJobs)) {
            return;
        }
        log.info("processPendingVideoEffectJobs found {} pending jobs", pendingJobs.size());

        for (VideoEffectJob job : pendingJobs) {
            try {
                GoEnhanceJobResponse jobResponse = goEnhanceClient.getJobStatus(job.getImgUuid());
                if (jobResponse == null) continue;

                if (jobResponse.isSuccess()) {
                    String videoUrl = jobResponse.getVideoUrl();
                    if (videoUrl != null) {
                        completeVideoEffectJob(job, videoUrl);
                        log.info("processPendingVideoEffectJobs completed job uuid={}", job.getImgUuid());
                    }
                } else if (!jobResponse.isPending() && !jobResponse.isProcessing()) {
                    job.setStatus(VideoEffectJobStatusEnum.FAILED);
                    videoEffectJobRepository.save(job);
                    log.error("processPendingVideoEffectJobs job failed uuid={} status={}", job.getImgUuid(),
                            job.getStatus());
                }
            } catch (Exception e) {
                log.error("processPendingVideoEffectJobs error for uuid={}", job.getImgUuid(), e);
            }
        }
    }

    private String pollForVideoUrl(String imgUuid) {
        int maxRetries = 20;
        int sleepMs = 3000;
        for (int i = 0; i < maxRetries; i++) {
            try {
                Thread.sleep(sleepMs);
                GoEnhanceJobResponse jobResponse = goEnhanceClient.getJobStatus(imgUuid);
                if (jobResponse == null) continue;
                if (jobResponse.isSuccess()) {
                    return jobResponse.getVideoUrl();
                }
                if (!jobResponse.isPending() && !jobResponse.isProcessing()) {
                    log.error("[GoEnhance] job failed uuid={} status={}", imgUuid,
                            jobResponse.getData() != null ? jobResponse.getData().getStatus() : "null");
                    return null;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            } catch (Exception e) {
                log.error("[GoEnhance] poll error uuid={}", imgUuid, e);
            }
        }
        log.warn("[GoEnhance] poll timed out uuid={}, job will be picked up by scheduler", imgUuid);
        return null;
    }

    private static final int VIDEO_EFFECT_PRIVATE_COST = 20;
    private static final int VIDEO_EFFECT_720P_COST = 200;

    @Override
    public List<VideoEffectConfigDTO> getVideoEffects() {
        return videoEffectConfigRepository.findAllByDeletedFalse().stream()
                .map(e -> new VideoEffectConfigDTO()
                        .setEffectId(e.getEffectId())
                        .setLabel(e.getLabel())
                        .setUrl(e.getUrl())
                        .setBaseCost(VIDEO_EFFECT_COST)
                        .setPrivateCost(VIDEO_EFFECT_PRIVATE_COST)
                        .setCost720p(VIDEO_EFFECT_720P_COST))
                .toList();
    }

    @Override
    public void refreshVideoEffects() {
        try {
            GoEnhanceEffectListResponse listResponse = goEnhanceClient.getEffectList();
            if (listResponse == null || listResponse.getCode() != 0 || CollectionUtils.isEmpty(listResponse.getData())) {
                log.error("refreshVideoEffects: empty or failed response from GoEnhance");
                return;
            }

            videoEffectConfigRepository.deleteAll();

            List<VideoEffectConfig> configs = listResponse.getData().stream()
                    .map(item -> new VideoEffectConfig()
                            .setEffectId(item.getEffectId())
                            .setLabel(item.getLabel())
                            .setUrl(item.getUrl()))
                    .toList();

            videoEffectConfigRepository.saveAll(configs);
            log.info("refreshVideoEffects: saved {} effects", configs.size());
        } catch (Exception e) {
            log.error("refreshVideoEffects failed", e);
        }
    }

    @Override
    public String reportImage (UserDTO userDTO, ImageReportRequest imageReportRequest) {
        if (Objects.isNull(imageReportRequest) || StringUtils.isEmpty(imageReportRequest.getGenId())) {
            return "SUCCESS";
        }
        String userCode = Objects.nonNull(userDTO) && StringUtils.isNotEmpty(userDTO.getCode()) ? userDTO.getCode() : null;
        ImageFlag imageFlag = new ImageFlag()
                .setGenId(imageReportRequest.getGenId())
                .setImageId(imageReportRequest.getImageId())
                .setReason(imageReportRequest.getReason())
                .setUserCode(userCode);
        imageFlagRepository.save(imageFlag);
        return "SUCCESS";
    }
}
