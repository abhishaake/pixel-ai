package com.av.pixel.enums;

import io.micrometer.common.util.StringUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum ImageRenderOptionEnum {

    TURBO("Turbo"),
    QUALITY("Quality");

    @Getter
    final String value;

    public static ImageRenderOptionEnum getEnumByName (String renderOption) {
        if (StringUtils.isEmpty(renderOption)) {
            return TURBO;
        }
        for (ImageRenderOptionEnum optionEnum : ImageRenderOptionEnum.values()) {
            if (optionEnum.value.equalsIgnoreCase(renderOption)) {
                return optionEnum;
            }
        }
        return TURBO;
    }
}
