package com.av.pixel.service.impl;

import com.av.pixel.cache.RLock;
import com.av.pixel.cache.Cache;
import com.av.pixel.client.IdeogramClient;
import com.av.pixel.dao.Generations;
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
import com.av.pixel.exception.IdeogramUnprocessableEntityException;
import com.av.pixel.helper.DateUtil;
import com.av.pixel.helper.GenerationHelper;
import com.av.pixel.helper.Validator;
import com.av.pixel.mapper.GenerationsMap;
import com.av.pixel.mapper.ModelConfigMap;
import com.av.pixel.mapper.UserCreditMap;
import com.av.pixel.mapper.ideogram.ImageMap;
import com.av.pixel.repository.ModelConfigRepository;
import com.av.pixel.request.GenerateRequest;
import com.av.pixel.request.GenerationsFilterRequest;
import com.av.pixel.request.ImageActionRequest;
import com.av.pixel.request.ImagePricingRequest;
import com.av.pixel.request.SortByRequest;
import com.av.pixel.request.ideogram.ImageRequest;
import com.av.pixel.response.GenerationsFilterResponse;
import com.av.pixel.response.ImagePricingResponse;
import com.av.pixel.response.ModelConfigResponse;
import com.av.pixel.response.ideogram.ImageResponse;
import com.av.pixel.service.AdminConfigService;
import com.av.pixel.service.GenerationsService;
import com.av.pixel.service.ImageCompressionService;
import com.av.pixel.service.LikeGenerationService;
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

import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Service
@Slf4j
@AllArgsConstructor
public class GenerationsServiceImpl implements GenerationsService {

    private final MongoTemplate mongoTemplate;
    private final UserCreditService userCreditService;
    private final ModelConfigRepository modelConfigRepository;
    private final IdeogramClient ideogramClient;
    private final GenerationHelper generationHelper;
    private final LikeGenerationService likeGenerationService;
    private final RLock locker;
    private final UserService userService;
    private final AdminConfigService adminConfigService;
    private final S3Service s3Service;
    private final ImageCompressionService imageCompressionService;

    @Override
    public GenerationsDTO generate (UserDTO userDTO, GenerateRequest generateRequest) {
        log.info("generate img req {} from {}", generateRequest.getPrompt(), userDTO.getCode());
        Validator.validateGenerateRequest(generateRequest);

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

            Integer availableCredits = userCreditDTO.getAvailable();
            Integer imageGenerationCost = getCost(generateRequest);

            if (availableCredits < imageGenerationCost) {
                throw new Error(HttpStatus.PAYMENT_REQUIRED, "Not enough credits");
            }

            ImageRequest imageRequest = ImageMap.validateAndGetImageRequest(generateRequest);

            List<ImageResponse> imageResponses = generateImage(imageRequest, userDTO.getCode());

            if (Objects.isNull(imageResponses)) {
                throw new Error("Some error occurred, please try again");
            }

            Generations generations = generationHelper.saveUserGeneration(userDTO.getCode(), generateRequest, imageRequest, imageResponses, imageGenerationCost);
            userCreditService.debitUserCredit(userDTO.getCode(), imageGenerationCost, OrderTypeEnum.IMAGE_GENERATION, "SERVER", generations.getId().toString());

            GenerationsDTO res = GenerationsMap.toGenerationsDTO(generations);
            assert res != null;
            res.setUserName(userDTO.getFirstName())
                .setUserImgUrl(userDTO.getImageUrl());

            locker.unlock(key);
            return res;
        }
        catch (Exception e) {
            Thread.currentThread().interrupt();
            locker.unlock(key);
            throw e;
        }
    }


    private List<ImageResponse> generateImage (ImageRequest imageRequest, String userCode) {
        List<ImageResponse> res = null;
        try {
            if (adminConfigService.isIdeogramClientDisabled(userCode)) {
                return generationHelper.generateImages(imageRequest);
            }
            res = ideogramClient.generateImages(imageRequest);
        } catch (IdeogramUnprocessableEntityException e) {
            throw new Error(e.getError());
        }
        catch (IdeogramException e) {
            return null;
        } catch (Exception e) {
            log.error("[CRITICAL] generate Image error : {}, for req {} ", e.getMessage(), imageRequest, e);
            return null;
        }
        try {
            uploadToS3(res, userCode);
            return res;
        }
        catch (Exception e){
            log.error("uploading error", e);
            return res;
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
        }

       if (!CollectionUtils.isEmpty(generationsFilterRequest.getStyles())) {
           generationsFilterRequest.setStyles(ImageStyleEnum.getEnumsForFilter(generationsFilterRequest.getStyles()));
       }

        Page<Generations> generationsPage = findByFilters(generationsFilterRequest.getUserCodes(),
                generationsFilterRequest.getCategories(),
                generationsFilterRequest.getStyles(),
                privacyEnum.getPrivateImage(),
                generationsFilterRequest.getSort(),
                PageRequest.of(generationsFilterRequest.getPage(), generationsFilterRequest.getSize()));

        long totalCount = generationsPage.getTotalElements();
        TreeSet<String> likedGenerations = null;
        if (Objects.nonNull(userDTO) && StringUtils.isNotEmpty(userDTO.getCode())) {
            List<String> genIds = generationsPage.getContent().stream().map(g -> g.getId().toString()).toList();
            likedGenerations = likeGenerationService.getLikedGenerationsByUserCode(userDTO.getCode(), genIds);
        }
        List<String> userCodes = generationsPage.getContent().stream().map(Generations::getUserCode).toList();
        Map<String, User> userMap = userService.getUserCodeVsUserMap(userCodes);

        return new GenerationsFilterResponse(GenerationsMap.toList(generationsPage.getContent(), likedGenerations, userMap),
                totalCount, generationsFilterRequest.getPage(), generationsPage.getNumberOfElements());
    }


    public Page<Generations> findByFilters (List<String> userCodes,
                                            List<String> categories,
                                            List<String> styles,
                                            Boolean privacy,
                                            SortByRequest sortByRequest,
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

        Query query = new Query();

        if (Objects.nonNull(sortByRequest) && StringUtils.isNotEmpty(sortByRequest.getSortBy())) {
            query.with(Sort.by(sortByRequest.getSortDir(), sortByRequest.getSortBy()));
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

        Integer finalCost = modelPricingDTO.getFinalCost(imagePricingRequest.getNoOfImages(),
                imagePricingRequest.isPrivateImage(), isSeed, StringUtils.isNotEmpty(imagePricingRequest.getNegativePrompt()));

        return new ImagePricingResponse()
                .setFinalCost(finalCost);
    }

    @Override
    public ModelConfigResponse getModelConfigs () {
        List<ModelConfig> modelConfigs = modelConfigRepository.findAllByDeletedFalse();

        if (CollectionUtils.isEmpty(modelConfigs)) {
            throw new Error("no model config found");
        }

        return new ModelConfigResponse()
                .setModels(ModelConfigMap.toList(modelConfigs));
    }

    @Override
    public String performAction (UserDTO userDTO, ImageActionRequest imageActionRequest) {
        String key = "action_" + imageActionRequest.getGenerationId();
        boolean locked = locker.tryLock(key, 1000);

        if (!locked) {
            return "success";
        }
        String res = "success";
        try {
            if (ImageActionEnum.LIKE.equals(imageActionRequest.getAction())) {
                res = likeGenerationService.likeGeneration(userDTO.getCode(), imageActionRequest.getGenerationId());
            } else if (ImageActionEnum.DISLIKE.equals(imageActionRequest.getAction())) {
                res = likeGenerationService.disLikeGeneration(userDTO.getCode(), imageActionRequest.getGenerationId());
            }
            return "success";
        } catch (Exception e) {
            Thread.currentThread().interrupt();
        } finally {
            locker.unlock(key);
        }
        return res;
    }
}
