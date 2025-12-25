package com.av.pixel.service.impl;

import com.av.pixel.cache.RLock;
import com.av.pixel.cache.Cache;
import com.av.pixel.client.IdeogramClient;
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
import com.av.pixel.service.UserCreditService;
import com.av.pixel.service.UserService;
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

@Service
@Slf4j
@AllArgsConstructor
public class GenerationsServiceImpl implements GenerationsService {

    private final MongoTemplate mongoTemplate;
    private final UserCreditService userCreditService;
    private final ModelConfigRepository modelConfigRepository;
    private final IdeogramClient ideogramClient;
    private final GenerationHelper generationHelper;
    private final GenerationActionService generationActionService;
    private final RLock locker;
    private final UserService userService;
    private final AdminConfigService adminConfigService;
    private final S3Service s3Service;
    private final ImageCompressionService imageCompressionService;
    private final ImageFlagRepository imageFlagRepository;
    private final EmailService emailService;
    private final AsyncUtil asyncUtil;
    private final BlockUserService blockUserService;

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

            Integer availableCredits = userCreditDTO.getAvailable();
            if (availableCredits < imageGenerationCost) {
                throw new Error(HttpStatus.PAYMENT_REQUIRED, "Not enough credits");
            }

            ImageRequest imageRequest = ImageMap.validateAndGetImageRequest(generateRequest,file);

            // Execute image generation asynchronously
            CompletableFuture<List<ImageResponse>> imageGenerationFuture = asyncUtil.executeAsync(() -> 
                generateImage(imageRequest, userDTO.getCode()));

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



    private List<ImageResponse> generateImage (ImageRequest imageRequest, String userCode) {
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
            emailService.sendErrorMail(body);
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
    public GenerationsFilterResponse filterImages (UserDTO userDTO, GenerationsFilterRequest generationsFilterRequest) {
        String userCode = (Objects.nonNull(userDTO) && StringUtils.isNotEmpty(userDTO.getCode())) ? userDTO.getCode() : null;

        Validator.validateFilterImageRequest(generationsFilterRequest, "");
        boolean isSelfProfile = StringUtils.isNotEmpty(userCode) && !CollectionUtils.isEmpty(generationsFilterRequest.getUserCodes())
                && generationsFilterRequest.getUserCodes().contains(userCode);

        ImagePrivacyEnum privacyEnum = ImagePrivacyEnum.getEnumByName(generationsFilterRequest.getPrivacy());

        if (isSelfProfile && ImagePrivacyEnum.DEFAULT.equals(privacyEnum)) {
            privacyEnum = ImagePrivacyEnum.BOTH;
        } else if (StringUtils.isNotEmpty(userCode) && List.of("P108", "P125").contains(userCode)) {
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
