package com.av.pixel.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum IdeogramModelEnum {

    V_1(false, true, false),
    V_1_TURBO(false, true, false),
    V_2(true, true, true),
    V_2_TURBO(true, true, true),
    V_2A(true, false, false),
    V_2A_TURBO(true, false, false);


    final boolean styleEnabled;
    final boolean isNegativePromptEnabled;
    final boolean isColorPaletteEnabled;

}
