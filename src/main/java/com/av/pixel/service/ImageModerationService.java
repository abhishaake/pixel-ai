package com.av.pixel.service;

import com.av.pixel.dto.ModerationResult;

public interface ImageModerationService {

    /**
     * Inspects an uploaded image for restricted content.
     *
     * <p>Never throws. A policy rejection and an infrastructure failure are both
     * ordinary return values, so callers have exactly one path to handle and a
     * provider outage can never surface as an unhandled error that skips the check.
     */
    ModerationResult moderate(byte[] imageBytes);
}
