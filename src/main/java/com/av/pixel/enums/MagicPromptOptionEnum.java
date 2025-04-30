package com.av.pixel.enums;

import io.micrometer.common.util.StringUtils;

public enum MagicPromptOptionEnum {

    AUTO,
    ON,
    OFF;

    public static MagicPromptOptionEnum getEnumByName (String promptOption) {
        if (StringUtils.isEmpty(promptOption)) {
            return AUTO;
        }
        for (MagicPromptOptionEnum optionEnum : MagicPromptOptionEnum.values()) {
            if (optionEnum.name().equalsIgnoreCase(promptOption)) {
                return optionEnum;
            }
        }
        return AUTO;
    }
}
