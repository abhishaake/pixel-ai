package com.av.pixel.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ImageCompressionConfig {

    KB_250(0.45f,0.9f),
    KB_500(0.3f,0.9f),
    KB_750(0.25f,0.9f),
    KB_1000(0.19f,0.85f),
    KB_1250(0.19f,0.85f),
    KB_1500(0.19f,0.8f),
    KB_1750(0.19f,0.8f),
    KB_2000(0.19f,0.75f),
    KB_2500(0.19f,0.8f),
    KB_4000(0.09f,0.8f),
    KB_10000(0.07f,0.8f);


    private final float scale;
    private final float quality;

    public static ImageCompressionConfig getBySize (double size) {
        if (size < 250) {
            return null;
        }
        if (size < 500) {
            return KB_250;
        }
        if (size < 750) {
            return KB_500;
        }
        if (size < 1000) {
            return KB_750;
        }
        if (size < 1250) {
            return KB_1000;
        }
        if (size < 1500) {
            return KB_1250;
        }
        if (size < 1750) {
            return KB_1500;
        }
        if (size < 2000) {
            return KB_1750;
        }
        if (size < 2500) {
            return KB_2000;
        }
        if (size < 4000) {
            return KB_2500;
        }
        if (size < 10000) {
            return KB_4000;
        }
        return KB_10000;
    }
}
