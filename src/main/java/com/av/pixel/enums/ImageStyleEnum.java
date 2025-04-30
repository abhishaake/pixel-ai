package com.av.pixel.enums;

import io.micrometer.common.util.StringUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum ImageStyleEnum {
    AUTO("Auto"),
    GENERAL("General"),
    REALISTIC("Realistic"),
    DESIGN("Design"),
    RENDER_3D("3D Art"),
    ANIME("Anime");

    @Getter
    final String value;

    public static ImageStyleEnum getEnumByValue (String style) {
        if (StringUtils.isEmpty(style)) {
            return null;
        }
        for (ImageStyleEnum styleEnum : ImageStyleEnum.values()) {
            if (styleEnum.value.equalsIgnoreCase(style)) {
                return styleEnum;
            }
        }
        return null;
    }

    public static ImageStyleEnum getEnumByName (String style) {
        if (StringUtils.isEmpty(style)) {
            return AUTO;
        }
        for (ImageStyleEnum styleEnum : ImageStyleEnum.values()) {
            if (styleEnum.name().equalsIgnoreCase(style)) {
                return styleEnum;
            }
        }
        return AUTO;
    }
}
