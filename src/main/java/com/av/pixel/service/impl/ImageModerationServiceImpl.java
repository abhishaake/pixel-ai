package com.av.pixel.service.impl;

import com.av.pixel.dto.ModerationResult;
import com.av.pixel.service.ImageModerationService;
import com.av.pixel.service.ModerationPolicy;
import com.av.pixel.service.ModerationPolicy.DetectedLabel;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.DetectModerationLabelsRequest;
import software.amazon.awssdk.services.rekognition.model.DetectModerationLabelsResponse;
import software.amazon.awssdk.services.rekognition.model.Image;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@Slf4j
public class ImageModerationServiceImpl implements ImageModerationService {

    /** Rekognition rejects inline image bytes larger than 5 MB. */
    private static final int MAX_BYTES = 5 * 1024 * 1024;

    /** Rekognition rejects images smaller than 80x80. */
    private static final int MIN_DIMENSION = 80;

    /** Longest edge to downscale to when the payload is over MAX_BYTES. */
    private static final int DOWNSCALE_EDGE = 1600;

    private final RekognitionClient rekognitionClient;
    private final boolean enabled;
    private final boolean failOpen;
    private final ModerationPolicy policy;

    public ImageModerationServiceImpl(
            RekognitionClient rekognitionClient,
            @Value("${moderation.enabled:true}") boolean enabled,
            @Value("${moderation.fail-open:false}") boolean failOpen,
            @Value("${moderation.blocked-labels:}") String blockedLabels) {
        this.rekognitionClient = rekognitionClient;
        this.enabled = enabled;
        this.failOpen = failOpen;
        this.policy = new ModerationPolicy(blockedLabels);
        log.info("image moderation enabled={} failOpen={}", enabled, failOpen);
    }

    @Override
    public ModerationResult moderate(byte[] imageBytes) {
        if (!enabled) {
            return ModerationResult.allowed();
        }
        if (imageBytes == null || imageBytes.length == 0) {
            log.error("[CRITICAL] moderation received empty image bytes");
            return ModerationResult.failure(failOpen);
        }
        try {
            byte[] normalised = normaliseForRekognition(imageBytes);

            DetectModerationLabelsResponse response = rekognitionClient.detectModerationLabels(
                    DetectModerationLabelsRequest.builder()
                            .image(Image.builder().bytes(SdkBytes.fromByteArray(normalised)).build())
                            .minConfidence((float) policy.lowestThreshold())
                            .build());

            List<DetectedLabel> detected = response.moderationLabels().stream()
                    .map(l -> new DetectedLabel(l.name(), l.parentName(), l.confidence()))
                    .toList();

            Optional<String> blocked = policy.firstBlocked(detected);
            if (blocked.isEmpty()) {
                return ModerationResult.allowed();
            }

            String topLabel = blocked.get();
            double topConfidence = detected.stream()
                    .filter(l -> topLabel.equals(l.name()))
                    .mapToDouble(DetectedLabel::confidence)
                    .max()
                    .orElse(0d);

            return ModerationResult.rejected(topLabel, topConfidence, describe(detected));
        } catch (Exception e) {
            log.error("[CRITICAL] image moderation failed, failOpen={}", failOpen, e);
            return ModerationResult.failure(failOpen);
        }
    }

    private static List<String> describe(List<DetectedLabel> detected) {
        return detected.stream()
                .map(l -> String.format(Locale.ROOT, "%s:%.1f", l.name(), l.confidence()))
                .toList();
    }

    /**
     * Rekognition accepts only JPEG and PNG, at most 5 MB inline, at least 80x80.
     * Re-encode and downscale so that an unusual or oversized upload is actually
     * checked rather than erroring into a fail-closed rejection.
     */
    private static byte[] normaliseForRekognition(byte[] imageBytes) throws Exception {
        BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(imageBytes));
        if (decoded == null) {
            throw new IllegalArgumentException("upload is not a decodable image");
        }
        if (decoded.getWidth() < MIN_DIMENSION || decoded.getHeight() < MIN_DIMENSION) {
            throw new IllegalArgumentException("upload is smaller than Rekognition's 80x80 minimum");
        }
        if (imageBytes.length <= MAX_BYTES) {
            return imageBytes;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Thumbnails.of(decoded)
                .size(DOWNSCALE_EDGE, DOWNSCALE_EDGE)
                .keepAspectRatio(true)
                .outputFormat("JPEG")
                .outputQuality(0.85f)
                .toOutputStream(out);
        byte[] resized = out.toByteArray();
        log.info("downscaled upload for moderation: {} bytes -> {} bytes", imageBytes.length, resized.length);
        return resized;
    }
}
