package com.av.pixel.request;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class GenerateRequest {
    String prompt;
    String aspectRatio;
    String model;
    String magicPromptOption;
    Long seed;
    String styleType;
    String negativePrompt;
    Integer noOfImages;
    String resolution;
    Boolean privateImage;
    String renderOption;
    String colorPalette;
}
