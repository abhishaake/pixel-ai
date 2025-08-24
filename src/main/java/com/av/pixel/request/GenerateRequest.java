package com.av.pixel.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@JsonIgnoreProperties(ignoreUnknown = true)
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
    Boolean haveCharacterFile;
}
