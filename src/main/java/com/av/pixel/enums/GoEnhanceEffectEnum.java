package com.av.pixel.enums;

import lombok.Getter;

@Getter
public enum GoEnhanceEffectEnum {

    HUG("hug"),
    KISS("kiss"),
    FRENCH_KISS("french-kiss"),
    BLOW_KISS("blow-kiss"),
    TWERK_DANCE("twerk-dance"),
    HIP_SHAKE("hip-shake"),
    JIGGLE_DANCE("jiggle-dance"),
    SQUAT_SHAKE("squat-shake"),
    PHUT_HON_DANCE("phut-hon-dance"),
    TURNING_METAL("turning-metal"),
    ANIME_2_REAL("anime2real"),
    MUSCLE_SHOW("muscle-show"),
    MERMAID_SPELL("mermaid-spell"),
    THUNDER_GOD("thunder-god"),
    SPITFIRE("spitfire"),
    SET_ON_FIRE("set-on-fire"),
    ALIEN_KIDNAP("alien-kidnap");

    private final String effectName;

    GoEnhanceEffectEnum(String effectName) {
        this.effectName = effectName;
    }
}
