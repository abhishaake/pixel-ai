package com.av.pixel.request.goenhance;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class GoEnhanceGenerateRequest {

    Args args;

    @Data
    @Accessors(chain = true)
    public static class Args {
        @JsonProperty("effect_id")
        String effectId;
        String resolution;
        @JsonProperty("reference_img")
        String referenceImg;
    }
}
