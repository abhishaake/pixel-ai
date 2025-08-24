package com.av.pixel.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum IdeogramModelEnum {

    V_1(false, true, false,false),
    V_1_TURBO(false, true, false,true),
    V_2(true, true, true,false),
    V_2_TURBO(true, true, true,true),
    V_2A(true, false, false,false),
    V_2A_TURBO(true, false, false,true),
    V_3_TURBO(true, false, false,true),
    V_3_QUALITY(true, false, false,false);


    final boolean styleEnabled;
    final boolean isNegativePromptEnabled;
    final boolean isColorPaletteEnabled;
    final boolean isTurboEnabled;

}
