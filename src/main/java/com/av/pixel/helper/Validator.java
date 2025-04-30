package com.av.pixel.helper;

import com.av.pixel.enums.AspectRatioEnum;
import com.av.pixel.enums.ColorPaletteEnum;
import com.av.pixel.enums.PixelModelEnum;
import com.av.pixel.exception.Error;
import com.av.pixel.repository.UserRepository;
import com.av.pixel.request.GenerateRequest;
import com.av.pixel.request.GenerationsFilterRequest;
import com.av.pixel.request.ImagePricingRequest;
import io.micrometer.common.util.StringUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Objects;

@Component
@Slf4j
@AllArgsConstructor
public class Validator {

    UserRepository userRepository;

    public static void validateNonEmpty(String str, String error) {
        if (StringUtils.isEmpty(str)) {
            throw new Error(error);
        }
    }

    public static void validateNonNull(Object obj, String error) {
        if (Objects.isNull(obj)) {
            throw new Error(error);
        }
    }

    public static void validateNonNull(Integer obj, String error) {
        if (Objects.isNull(obj)) {
            throw new Error(error);
        }
    }

    public static void validateFilterImageRequest(GenerationsFilterRequest filterRequest, String error) {
        if (Objects.isNull(filterRequest)) {
            throw new Error(error);
        }
        if (Objects.isNull(filterRequest.getPage())){
            filterRequest.setPage(0);
        }
        if (Objects.isNull(filterRequest.getSize())){
            filterRequest.setSize(10);
        }
    }

    public static void validateGenerateRequest (GenerateRequest generateRequest) {
        validateNonNull(generateRequest, "Request cannot be empty");
        validateNonEmpty(generateRequest.getPrompt(), "Please provide valid prompt");
        validateNonNull(generateRequest.getNoOfImages(), "Please provide number of images between range 1 and 4");
        validateNoOfImageRange(generateRequest.getNoOfImages(), "Please provide number of images between range 1 and 4");
        validateImageColorPalette(generateRequest);
    }

    private static void validateImageColorPalette(GenerateRequest generateRequest) {
        if (StringUtils.isEmpty(generateRequest.getColorPalette())) {
            return;
        }
        ColorPaletteEnum colorPaletteEnum = ColorPaletteEnum.getEnumByName(generateRequest.getColorPalette());
        generateRequest.setColorPalette(colorPaletteEnum.name());
    }


    private static void validateNoOfImageRange (Integer noOfImages, String error) {
        if ((noOfImages < 1) || (noOfImages >= 5)) {
            throw new Error(error);
        }
    }

    private static void validateSeedRange (Long seed, String error) {
        if (Objects.nonNull(seed) && ((seed < 0) || (seed > 2147483647))){
            throw new Error(error);
        }
    }

    public static void validateModelPricingRequest (ImagePricingRequest imagePricingRequest) {
        validateNonNull(imagePricingRequest, "Request cannot be empty");
        validateNonEmpty(imagePricingRequest.getModel(), "Please provide model");
        validateNonNull(imagePricingRequest.getNoOfImages(), "Please provide number of images");
        validateNoOfImageRange(imagePricingRequest.getNoOfImages(), "Please provide number of image in between 1 and 4");
        validateSeedRange(imagePricingRequest.getSeed(), "Please provide seed in between range 1 and 2147483647");
    }
}
