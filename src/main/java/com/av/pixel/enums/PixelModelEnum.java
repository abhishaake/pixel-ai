package com.av.pixel.enums;

import io.micrometer.common.util.StringUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum PixelModelEnum {

    PIXEL_V1("Pixel 1.0", IdeogramModelEnum.V_1_TURBO, IdeogramModelEnum.V_1, false),
    PIXEL_V2("Pixel 2.0", IdeogramModelEnum.V_2_TURBO, IdeogramModelEnum.V_2, true),
    PIXEL_V2A("Pixel 3.0", IdeogramModelEnum.V_2A_TURBO, IdeogramModelEnum.V_2A, true),
    PIXEL_V3("Pixel 4.0", IdeogramModelEnum.V_3_TURBO, IdeogramModelEnum.V_3_QUALITY, true),
    ;

    @Getter
    final String value;
    final IdeogramModelEnum turboModel;
    final IdeogramModelEnum qualityModel;
    final boolean styleEnabled;

    public static IdeogramModelEnum getIdeogramModelByNameAndRenderOption (String modelName, ImageRenderOptionEnum renderOptionEnum) {
        if (StringUtils.isEmpty(modelName)) {
            return null;
        }
        for (PixelModelEnum modelEnum : PixelModelEnum.values()) {
            if (modelEnum.name().equalsIgnoreCase(modelName)) {
                if (ImageRenderOptionEnum.QUALITY.equals(renderOptionEnum)) {
                    return modelEnum.qualityModel;
                }
                return modelEnum.turboModel;
            }
        }
        return null;
    }

    public static PixelModelEnum getModelByValue (String name) {
        if (StringUtils.isEmpty(name)) {
            return null;
        }
        for (PixelModelEnum modelEnum : PixelModelEnum.values()) {
            if (modelEnum.value.equalsIgnoreCase(name)) {
                return modelEnum;
            }
        }
        return null;
    }

    public static PixelModelEnum getModelByName (String name) {
        if (StringUtils.isEmpty(name)) {
            return null;
        }
        for (PixelModelEnum modelEnum : PixelModelEnum.values()) {
            if (modelEnum.name().equalsIgnoreCase(name)) {
                return modelEnum;
            }
        }
        return null;
    }
}
