package com.av.pixel.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Decides whether Rekognition's moderation labels violate the upload policy.
 *
 * <p>Deliberately free of AWS SDK types so the rules — the part most likely to be
 * retuned while the app is under Play review — can be tested without mocks.
 */
@Slf4j
public class ModerationPolicy {

    /** Returned by {@link #lowestThreshold()} when nothing is blocked, so Rekognition returns as little as possible. */
    private static final double NO_BLOCKLIST_THRESHOLD = 100.0d;

    private final Map<String, Double> thresholdsByLabel;

    public ModerationPolicy(String blockedLabelsProperty) {
        this.thresholdsByLabel = parse(blockedLabelsProperty);
        log.info("moderation policy loaded with {} blocked labels: {}", thresholdsByLabel.size(), thresholdsByLabel);
    }

    private static Map<String, Double> parse(String property) {
        Map<String, Double> parsed = new LinkedHashMap<>();
        if (StringUtils.isBlank(property)) {
            return parsed;
        }
        for (String entry : property.split(",")) {
            String[] parts = entry.split(":");
            if (parts.length != 2) {
                log.warn("ignoring malformed moderation.blocked-labels entry: {}", entry);
                continue;
            }
            String label = parts[0].trim();
            try {
                parsed.put(label.toLowerCase(), Double.parseDouble(parts[1].trim()));
            } catch (NumberFormatException e) {
                log.warn("ignoring moderation.blocked-labels entry with non-numeric threshold: {}", entry);
            }
        }
        return parsed;
    }

    /**
     * The smallest configured confidence, suitable as Rekognition's MinConfidence so
     * that every label we might act on is returned and nothing else is.
     */
    public double lowestThreshold() {
        return thresholdsByLabel.values().stream()
                .min(Double::compareTo)
                .orElse(NO_BLOCKLIST_THRESHOLD);
    }

    /**
     * @return the name of the highest-confidence label that violates the policy, or
     *         empty when every label is acceptable.
     */
    public Optional<String> firstBlocked(List<DetectedLabel> labels) {
        if (labels == null || labels.isEmpty()) {
            return Optional.empty();
        }
        return labels.stream()
                .filter(this::isBlocked)
                .max(Comparator.comparingDouble(DetectedLabel::confidence))
                .map(DetectedLabel::name);
    }

    private boolean isBlocked(DetectedLabel label) {
        return exceedsThreshold(label.name(), label.confidence())
                || exceedsThreshold(label.parentName(), label.confidence());
    }

    private boolean exceedsThreshold(String name, double confidence) {
        if (StringUtils.isBlank(name)) {
            return false;
        }
        Double threshold = thresholdsByLabel.get(name.toLowerCase());
        return threshold != null && confidence >= threshold;
    }

    /** One moderation label, decoupled from the AWS SDK's ModerationLabel type. */
    public record DetectedLabel(String name, String parentName, double confidence) {
    }
}
