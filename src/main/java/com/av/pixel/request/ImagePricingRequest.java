package com.av.pixel.request;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class ImagePricingRequest {

    String model;

    Integer noOfImages;

    String renderOption;

    Long seed;

    boolean privateImage;

    String negativePrompt;
}
