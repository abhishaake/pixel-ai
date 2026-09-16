package com.av.pixel.service;

import com.av.pixel.dao.Generations;
import com.av.pixel.enums.ModerationSourceEnum;
import org.springframework.web.multipart.MultipartFile;

/**
 * Enforces the content policy at the points where content becomes publicly visible.
 *
 * <p>Distinct from {@link ImageModerationService}, which only answers "does this one
 * image violate the policy". This service decides <em>what</em> to check and <em>what
 * to do</em> about a violation: refuse the request, record it, and raise the alarm.
 */
public interface ContentModerationService {

    /**
     * Checks a reference image being uploaded alongside a generation request.
     *
     * <p>Only public generations are checked. A private generation is visible to
     * nobody but its owner, so its upload is not sent to Rekognition.
     *
     * @throws com.av.pixel.exception.Error 422 when the upload may not be used
     */
    void assertUploadAllowed(String userCode, MultipartFile file, ModerationSourceEnum source, boolean isPublic);

    /**
     * Checks an existing generation that is about to become publicly visible.
     *
     * <p>Covers every asset the feed exposes: each generated image, and the character
     * reference image, which {@code GenerationsDTO} also returns.
     *
     * @throws com.av.pixel.exception.Error 422 when the generation may not be made public
     */
    void assertGenerationAllowedPublic(String userCode, Generations generation);
}
