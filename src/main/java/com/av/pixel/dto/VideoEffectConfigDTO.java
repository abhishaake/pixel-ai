package com.av.pixel.dto;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class VideoEffectConfigDTO {
    String effectId;
    String label;
    String url;
    Integer baseCost;
    Integer privateCost;
    Integer cost720p;
}
