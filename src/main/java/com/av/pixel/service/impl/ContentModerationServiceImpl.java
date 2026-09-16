package com.av.pixel.service.impl;

import com.av.pixel.dao.Generations;
import com.av.pixel.dao.PromptImage;
import com.av.pixel.dao.UploadModerationLog;
import com.av.pixel.dto.ModerationResult;
import com.av.pixel.enums.ModerationDecisionEnum;
import com.av.pixel.enums.ModerationSourceEnum;
import com.av.pixel.exception.Error;
import com.av.pixel.helper.DateUtil;
import com.av.pixel.repository.UploadModerationLogRepository;
import com.av.pixel.service.ContentModerationService;
import com.av.pixel.service.ImageModerationService;
import com.av.pixel.service.S3Service;
import com.av.pixel.service.SesEmailService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

@Service
@Slf4j
@AllArgsConstructor
public class ContentModerationServiceImpl implements ContentModerationService {

    private static final String REJECTED_MESSAGE = "This image does not comply with our content policy. Please upload a different image.";
    private static final long ALERT_INTERVAL_MS = 5 * 60 * 1000L;

    private final ImageModerationService imageModerationService;
    private final UploadModerationLogRepository uploadModerationLogRepository;
    private final S3Service s3Service;
    private final SesEmailService sesEmailService;

    private final AtomicLong lastAlertAt = new AtomicLong(0L);

    @Override
    public void assertUploadAllowed(String userCode, MultipartFile file, ModerationSourceEnum source, boolean isPublic) {
        if (file == null || file.isEmpty()) {
            return;
        }
        if (!isPublic) {
            // A private generation is visible to nobody but its owner, so it is not
            // sent to Rekognition. Making it public later goes through
            // assertGenerationAllowedPublic, which checks it then.
            log.debug("skipping moderation of private upload, user={} source={}", userCode, source);
            return;
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (Exception e) {
            log.error("[CRITICAL] could not read upload for moderation, user={}", userCode, e);
            refuse(uploadLog(userCode, file, source, ModerationDecisionEnum.ERROR, null));
            return;
        }

        ModerationResult result = imageModerationService.moderate(bytes);
        if (result.isFailure()) {
            alert("upload moderation unavailable, source " + source, userCode);
        }
        if (result.isAllowed()) {
            return;
        }

        ModerationDecisionEnum decision = decisionFor(result);
        log.warn("upload rejected user={} source={} decision={} label={}",
                userCode, source, decision, result.getTopLabel());
        refuse(uploadLog(userCode, file, source, decision, result));
    }

    @Override
    public void assertGenerationAllowedPublic(String userCode, Generations generation) {
        if (Objects.isNull(generation)) {
            return;
        }
        String generationId = Objects.nonNull(generation.getId()) ? generation.getId().toString() : null;

        List<String> urls;
        try {
            urls = publiclyVisibleUrls(generation);
        } catch (UncheckableAssetException e) {
            log.error("[CRITICAL] refusing to publish generation={} : {}", generationId, e.getMessage());
            refuse(generationLog(userCode, generationId, ModerationDecisionEnum.ERROR, null));
            return;
        }

        for (String url : urls) {
            byte[] bytes;
            try {
                bytes = s3Service.downloadImage(url).body();
            } catch (Exception e) {
                log.error("[CRITICAL] could not download {} for moderation, generation={}", url, generationId, e);
                bytes = null;
            }
            if (bytes == null || bytes.length == 0) {
                alert("could not download generation asset for moderation: " + url, userCode);
                refuse(generationLog(userCode, generationId, ModerationDecisionEnum.ERROR, null));
                return;
            }

            ModerationResult result = imageModerationService.moderate(bytes);
            if (result.isFailure()) {
                alert("generation moderation unavailable", userCode);
            }
            if (result.isAllowed()) {
                continue;
            }

            ModerationDecisionEnum decision = decisionFor(result);
            log.warn("generation refused for public user={} generation={} decision={} label={}",
                    userCode, generationId, decision, result.getTopLabel());
            refuse(generationLog(userCode, generationId, decision, result));
            return;
        }
    }

    /**
     * Every asset the public feed exposes for this generation.
     *
     * <p>Video generations contribute their thumbnail rather than their file:
     * Rekognition's DetectModerationLabels reads still images only, and the thumbnail
     * is a real frame of the clip.
     */
    private List<String> publiclyVisibleUrls(Generations generation) {
        List<String> urls = new ArrayList<>();
        boolean isVideo = Boolean.TRUE.equals(generation.getVideoEffect());

        if (StringUtils.isNotBlank(generation.getCharacterRefImageUrl())) {
            urls.add(generation.getCharacterRefImageUrl());
        }
        if (CollectionUtils.isEmpty(generation.getImages())) {
            return urls;
        }
        for (PromptImage image : generation.getImages()) {
            String url = isVideo ? image.getThumbnail() : image.getUrl();
            if (StringUtils.isNotBlank(url)) {
                urls.add(url);
                continue;
            }
            if (isVideo) {
                // No frame to inspect means no way to clear the clip. Refuse rather
                // than publish a video nothing has looked at.
                throw new UncheckableAssetException(image.getUrl());
            }
        }
        return urls;
    }

    private static ModerationDecisionEnum decisionFor(ModerationResult result) {
        return result.isFailure() ? ModerationDecisionEnum.ERROR : ModerationDecisionEnum.REJECTED;
    }

    private UploadModerationLog uploadLog(String userCode, MultipartFile file, ModerationSourceEnum source,
                                          ModerationDecisionEnum decision, ModerationResult result) {
        return base(userCode, source, decision, result)
                .setContentType(file.getContentType())
                .setSizeBytes(file.getSize());
    }

    private UploadModerationLog generationLog(String userCode, String generationId,
                                              ModerationDecisionEnum decision, ModerationResult result) {
        return base(userCode, ModerationSourceEnum.PRIVACY_TOGGLE, decision, result)
                .setGenerationId(generationId);
    }

    private UploadModerationLog base(String userCode, ModerationSourceEnum source,
                                     ModerationDecisionEnum decision, ModerationResult result) {
        return new UploadModerationLog()
                .setUserCode(userCode)
                .setSource(source)
                .setDecision(decision)
                .setTopLabel(result == null ? null : result.getTopLabel())
                .setTopConfidence(result == null ? null : result.getTopConfidence())
                .setLabels(result == null ? List.of() : result.getLabels());
    }

    /** Persists the refusal, then refuses. The throw happens either way. */
    private void refuse(UploadModerationLog entry) {
        try {
            uploadModerationLogRepository.save(entry);
        } catch (Exception e) {
            // Never let an audit-write failure turn a refusal into a 500 that lets the content through.
            log.error("could not persist moderation log for user={}", entry.getUserCode(), e);
        }
        throw new Error(HttpStatus.UNPROCESSABLE_ENTITY, REJECTED_MESSAGE);
    }

    /** At most one mail per interval, so a Rekognition outage cannot flood the inbox. */
    private void alert(String what, String userCode) {
        long now = DateUtil.currentTimeMillis();
        long last = lastAlertAt.get();
        if (now - last < ALERT_INTERVAL_MS || !lastAlertAt.compareAndSet(last, now)) {
            return;
        }
        try {
            sesEmailService.sendErrorMail("[CRITICAL] content moderation problem"
                    + "\n\n detail : " + what
                    + "\n\n user Code : " + userCode
                    + "\n\n Content is being refused while moderation is degraded (fail-closed).");
        } catch (Exception e) {
            log.error("could not send moderation alert", e);
        }
    }

    /** A video with no thumbnail: there is no still frame Rekognition could read. */
    private static class UncheckableAssetException extends RuntimeException {
        UncheckableAssetException(String url) {
            super("no still frame available for " + url);
        }
    }
}
