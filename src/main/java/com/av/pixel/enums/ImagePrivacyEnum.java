package com.av.pixel.enums;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ImagePrivacyEnum {
    DEFAULT("default", false),
    PUBLIC("public", false),
    PRIVATE("private", true),
    BOTH("both", null);

    final String value;
    final Boolean privateImage;

    public static ImagePrivacyEnum getEnumByName (String privacy) {
        for (ImagePrivacyEnum privacyEnum : ImagePrivacyEnum.values()) {
            if (privacyEnum.value.equalsIgnoreCase(privacy)) {
                return privacyEnum;
            }
        }
        return DEFAULT;
    }
}
