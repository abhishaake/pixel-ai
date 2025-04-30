package com.av.pixel.request.ideogram;

import com.av.pixel.enums.AspectRatioEnum;
import com.av.pixel.enums.IdeogramModelEnum;
import com.av.pixel.enums.ImageStyleEnum;
import com.av.pixel.enums.MagicPromptOptionEnum;
import com.av.pixel.enums.ResolutionEnum;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Accessors(chain = true)
public class ImageRequest {

    String prompt;

    @JsonProperty("aspect_ratio")
    AspectRatioEnum aspectRatio;

    IdeogramModelEnum model;

    @JsonProperty("magic_prompt_option")
    MagicPromptOptionEnum magicPromptOption;

    Long seed;

    @JsonProperty("style_type")
    ImageStyleEnum styleType;

    @JsonProperty("negative_prompt")
    String negativePrompt;

    @JsonProperty("num_images")
    Integer numberOfImages;

    ResolutionEnum resolution;

    @JsonProperty("color_palette")
    ColorPalette colorPalette;
}
