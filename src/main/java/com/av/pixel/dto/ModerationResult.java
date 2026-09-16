package com.av.pixel.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/**
 * The verdict on one uploaded image.
 *
 * <p>{@code failure} distinguishes "the moderator said no" from "the moderator could
 * not be reached". When {@code failure} is true, {@code allowed} reflects the
 * configured fail-open setting rather than any judgement about the image.
 */
@Getter
@AllArgsConstructor
public class ModerationResult {

    private final boolean allowed;
    private final boolean failure;
    private final String topLabel;
    private final Double topConfidence;
    private final List<String> labels;

    public static ModerationResult allowed() {
        return new ModerationResult(true, false, null, null, List.of());
    }

    public static ModerationResult rejected(String topLabel, double topConfidence, List<String> labels) {
        return new ModerationResult(false, false, topLabel, topConfidence, labels);
    }

    public static ModerationResult failure(boolean allowed) {
        return new ModerationResult(allowed, true, null, null, List.of());
    }
}
