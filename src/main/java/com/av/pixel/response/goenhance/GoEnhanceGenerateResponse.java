package com.av.pixel.response.goenhance;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class GoEnhanceGenerateResponse {

    @JsonProperty("img_uuid")
    String imgUuid;
}
