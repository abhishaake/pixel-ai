package com.av.pixel.service.impl;

import com.av.pixel.cache.RLock;
import com.av.pixel.dao.Generations;
import com.av.pixel.dto.UserCreditDTO;
import com.av.pixel.dto.UserDTO;
import com.av.pixel.enums.OrderTypeEnum;
import com.av.pixel.exception.Error;
import com.av.pixel.request.UpdateImagePrivacyRequest;
import com.av.pixel.response.ImagePrivacyResponse;
import com.av.pixel.service.AdminConfigService;
import com.av.pixel.service.ImagePrivacyService;
import com.av.pixel.service.UserCreditService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@Slf4j
@AllArgsConstructor
public class ImagePrivacyServiceImpl implements ImagePrivacyService {

    private final MongoTemplate mongoTemplate;
    private final UserCreditService userCreditService;
    private final AdminConfigService adminConfigService;
    private final RLock locker;

    @Override
    public ImagePrivacyResponse updateImagePrivacy (UserDTO userDTO, UpdateImagePrivacyRequest request) {

        if (Objects.isNull(request) || StringUtils.isEmpty(request.getGenerationId())
                || Objects.isNull(request.getPrivateImage())) {
            throw new Error(HttpStatus.BAD_REQUEST, "generationId and privateImage are required");
        }

        ObjectId generationObjectId;
        try {
            generationObjectId = new ObjectId(request.getGenerationId());
        } catch (IllegalArgumentException e) {
            throw new Error(HttpStatus.BAD_REQUEST, "Invalid generationId");
        }

        String userCode = Objects.nonNull(userDTO) ? userDTO.getCode() : null;
        if (StringUtils.isEmpty(userCode)) {
            throw new Error(HttpStatus.FORBIDDEN, "Not allowed");
        }

        Generations generation = findGeneration(generationObjectId);
        if (Objects.isNull(generation)) {
            throw new Error(HttpStatus.NOT_FOUND, "Image not found");
        }

        if (!userCode.equals(generation.getUserCode())) {
            throw new Error(HttpStatus.FORBIDDEN, "You can only change privacy of your own images");
        }

        boolean makePrivate = Boolean.TRUE.equals(request.getPrivateImage());

        if (isUnlocked(generation)) {
            return applyFree(generation, makePrivate, userCode);
        }

        if (!makePrivate) {
            // Locked and already public: nothing to change and nothing to charge for.
            return new ImagePrivacyResponse()
                    .setGenerationId(request.getGenerationId())
                    .setPrivateImage(false)
                    .setPrivacyUnlocked(false)
                    .setChargedCredits(0)
                    .setAvailableCredits(currentBalance(userCode));
        }

        return unlockAndApply(userCode, generation);
    }

    private ImagePrivacyResponse applyFree (Generations generation, boolean makePrivate, String userCode) {
        generation.setPrivateImage(makePrivate);
        generation.setPrivacyUnlocked(true);
        mongoTemplate.save(generation);

        return new ImagePrivacyResponse()
                .setGenerationId(generation.getId().toString())
                .setPrivateImage(makePrivate)
                .setPrivacyUnlocked(true)
                .setChargedCredits(0)
                .setAvailableCredits(currentBalance(userCode));
    }

    private ImagePrivacyResponse unlockAndApply (String userCode, Generations generation) {
        // Keyed on the user, not the generation, so concurrent unlocks of *different*
        // images cannot each pass the balance check and overdraw the account.
        // Mirrors the "generation_" + userCode lock in GenerationsServiceImpl.generate.
        String key = "privacy_unlock_" + userCode;
        boolean locked = locker.tryLock(key, 10);

        if (!locked) {
            throw new Error(HttpStatus.CONFLICT, "Privacy change already in progress, please wait..");
        }

        try {
            Integer cost = adminConfigService.getPrivacyUnlockCost();

            UserCreditDTO credit = userCreditService.getUserCredit(userCode);
            Integer available = Objects.nonNull(credit) && Objects.nonNull(credit.getAvailable())
                    ? credit.getAvailable() : 0;

            if (available < cost) {
                throw new Error(HttpStatus.PAYMENT_REQUIRED, "Not enough credits");
            }

            generation.setPrivateImage(true);
            generation.setPrivacyUnlocked(true);
            mongoTemplate.save(generation);

            // Saved before debiting on purpose: if the debit fails the user gets one free
            // unlock, which is preferable to charging them for a change that never landed.
            UserCreditDTO updated;
            try {
                updated = userCreditService.debitUserCredit(userCode, cost, OrderTypeEnum.PRIVACY_UNLOCK,
                        "SERVER", generation.getId().toString());
            } catch (Exception e) {
                log.error("privacy unlock debit failed after save, userCode : {} , generationId : {} , cost : {}",
                        userCode, generation.getId(), cost, e);
                return new ImagePrivacyResponse()
                        .setGenerationId(generation.getId().toString())
                        .setPrivateImage(true)
                        .setPrivacyUnlocked(true)
                        .setChargedCredits(0)
                        .setAvailableCredits(available);
            }

            return new ImagePrivacyResponse()
                    .setGenerationId(generation.getId().toString())
                    .setPrivateImage(true)
                    .setPrivacyUnlocked(true)
                    .setChargedCredits(cost)
                    .setAvailableCredits(Objects.nonNull(updated) ? updated.getAvailable() : available - cost);
        } finally {
            locker.unlock(key);
        }
    }

    private Generations findGeneration (ObjectId generationObjectId) {
        Query query = new Query(Criteria.where("_id").is(generationObjectId)
                .and("deleted").is(false));
        return mongoTemplate.findOne(query, Generations.class);
    }

    private boolean isUnlocked (Generations generation) {
        return Boolean.TRUE.equals(generation.getPrivacyUnlocked())
                || Boolean.TRUE.equals(generation.getPrivateImage());
    }

    private Integer currentBalance (String userCode) {
        UserCreditDTO credit = userCreditService.getUserCredit(userCode);
        return Objects.nonNull(credit) ? credit.getAvailable() : 0;
    }
}
