package com.av.pixel.dto;

import com.av.pixel.enums.ImageStyleEnum;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class GenerationsDTO {
    String generationId;
    String creationEpoch;
    String userCode;
    String userName;
    String userImgUrl;
    List<PromptImageDTO> images;
    String tag;
    String category;
    String model;
    String userPrompt;
    Long likes;
    String renderOption;
    Long seed;
    String resolution;
    Boolean privateImage;
    String style;
    boolean selfLike;
    String colorPalette;
    String aspectRatio;

    public String getStyle () {
        ImageStyleEnum styleEnum = ImageStyleEnum.getEnumByName(this.style);
        return styleEnum.getValue();
    }
}
