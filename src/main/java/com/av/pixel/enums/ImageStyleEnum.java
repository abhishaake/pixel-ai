package com.av.pixel.enums;

import io.micrometer.common.util.StringUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.checkerframework.checker.units.qual.A;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

    public static List<String> getEnumsForFilter (List<String> list) {
        if (CollectionUtils.isEmpty(list)) {
            return Collections.emptyList();
        }
        List<String> finalList = new ArrayList<>();
        for(String str : list) {
            for (ImageStyleEnum styleEnum : ImageStyleEnum.values()) {
                if (styleEnum.value.equalsIgnoreCase(str) || styleEnum.name().equalsIgnoreCase(str)) {
                    finalList.add(styleEnum.name());
                }
            }
        }
        return finalList;
    }
}
