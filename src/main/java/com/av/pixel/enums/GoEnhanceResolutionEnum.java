package com.av.pixel.enums;

import lombok.Getter;

@Getter
public enum GoEnhanceResolutionEnum {

    P_480("480p"),
    P_540("540p"),
    P_720("720p");

    private final String value;

    GoEnhanceResolutionEnum(String value) {
        this.value = value;
    }

    public static GoEnhanceResolutionEnum fromValue(String value) {
        if (value == null) return P_720;
        for (GoEnhanceResolutionEnum r : values()) {
            if (r.value.equalsIgnoreCase(value)) return r;
        }
        return P_720;
    }
}
