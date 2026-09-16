package com.av.pixel.enums;

public enum ModerationSourceEnum {
    /** Reference image uploaded to a public image generation. */
    IMAGE_GENERATION,
    /** Reference image uploaded to a public video effect. */
    VIDEO_EFFECT,
    /** An existing generation being switched from private to public. */
    PRIVACY_TOGGLE
}
