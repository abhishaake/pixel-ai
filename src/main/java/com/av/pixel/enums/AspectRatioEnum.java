package com.av.pixel.enums;

import io.micrometer.common.util.StringUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum AspectRatioEnum {
    ASPECT_10_16("10:16"),
    ASPECT_16_10("16:10"),
    ASPECT_9_16("9:16"),
    ASPECT_16_9("16:9"),
    ASPECT_3_2("3:2"),
    ASPECT_2_3("2:3"),
    ASPECT_4_3("4:3"),
    ASPECT_3_4("3:4"),
    ASPECT_1_1("1:1"),
    ASPECT_1_3("1:3"),
    ASPECT_3_1("3:1");

    @Getter
    final String value;

    public static AspectRatioEnum getEnumByName (String ratioName) {
        if (StringUtils.isEmpty(ratioName)) {
            return ASPECT_1_1;
        }
        for (AspectRatioEnum ratioEnum : AspectRatioEnum.values()) {
            if (ratioEnum.value.equalsIgnoreCase(ratioName)) {
                return ratioEnum;
            }
        }
        return ASPECT_1_1;
    }

}
