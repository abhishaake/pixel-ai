package com.av.pixel.response.ideogram;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ImageResponse {
    String prompt;

    String resolution;

    @JsonProperty("is_image_safe")
    Boolean isImageSafe;

    Long seed;

    String url;

    @JsonProperty("style_type")
    String styleType;
}
