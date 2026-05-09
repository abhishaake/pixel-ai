package com.av.pixel.response.goenhance;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class GoEnhanceEffectListResponse {

    int code;
    String msg;
    List<EffectItem> data;

    @Data
    public static class EffectItem {
        String label;
        String url;
        @JsonProperty("effect_id")
        String effectId;
    }
}
