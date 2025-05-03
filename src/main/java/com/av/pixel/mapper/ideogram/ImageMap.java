package com.av.pixel.mapper.ideogram;

import com.av.pixel.enums.AspectRatioEnum;
import com.av.pixel.enums.IdeogramModelEnum;
import com.av.pixel.enums.ImageRenderOptionEnum;
import com.av.pixel.enums.ImageStyleEnum;
import com.av.pixel.enums.MagicPromptOptionEnum;
import com.av.pixel.enums.PixelModelEnum;
import com.av.pixel.enums.ResolutionEnum;
import com.av.pixel.exception.Error;
import com.av.pixel.request.GenerateRequest;
import com.av.pixel.request.ideogram.ColorPalette;
import com.av.pixel.request.ideogram.ImageRequest;

import java.util.Objects;

public class ImageMap {

    public static ImageRequest validateAndGetImageRequest (GenerateRequest generateRequest) {
        AspectRatioEnum aspectRatio = AspectRatioEnum.getEnumByName(generateRequest.getAspectRatio());
        ImageRenderOptionEnum renderOption = ImageRenderOptionEnum.getEnumByName(generateRequest.getRenderOption());
        IdeogramModelEnum model = PixelModelEnum.getIdeogramModelByNameAndRenderOption(generateRequest.getModel(), renderOption);

        if (Objects.isNull(model)) {
            throw new Error("Please provide valid model name");
        }

        MagicPromptOptionEnum magicPromptOption = MagicPromptOptionEnum.getEnumByName(generateRequest.getMagicPromptOption());
        ImageStyleEnum imageStyle = ImageStyleEnum.getEnumByValue(generateRequest.getStyleType());

        return new ImageRequest()
                .setNumberOfImages(generateRequest.getNoOfImages())
                .setAspectRatio(aspectRatio)
                .setModel(model)
                .setMagicPromptOption(magicPromptOption)
                .setPrompt(generateRequest.getPrompt())
                .setSeed(generateRequest.getSeed())
                .setNegativePrompt(model.isNegativePromptEnabled() ? generateRequest.getNegativePrompt() : null)
                .setResolution(null)
                .setStyleType(model.isStyleEnabled() ? imageStyle : null)
                .setColorPalette((model.isColorPaletteEnabled() && Objects.nonNull(generateRequest.getColorPalette())) ? new ColorPalette().setName(generateRequest.getColorPalette()) : null);
    }

}
