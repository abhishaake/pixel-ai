package com.av.pixel.enums;

import io.micrometer.common.util.StringUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ColorPaletteEnum {
    EMBER("EMBER"),
    FRESH("FRESH"),
    JUNGLE("JUNGLE"),
    MAGIC("MAGIC"),
    MELON("MELON"),
    MOSAIC("MOSAIC"),
    PASTEL("PASTEL"),
    ULTRAMARINE("ULTRAMARINE"),
    AUTO(null);

    private final String value;

    public static ColorPaletteEnum getEnumByName (String name) {
        if (StringUtils.isEmpty(name)) {
            return AUTO;
        }
        for (ColorPaletteEnum paletteEnum : ColorPaletteEnum.values()) {
            if (paletteEnum.name().equalsIgnoreCase(name)) {
                return paletteEnum;
            }
        }
        return AUTO;
    }
}
