package com.av.pixel.mapper;

import com.av.pixel.dao.Generations;
import com.av.pixel.dao.PromptImage;
import com.av.pixel.dao.User;
import com.av.pixel.dto.GenerationsDTO;
import com.av.pixel.dto.PromptImageDTO;
import com.av.pixel.enums.AspectRatioEnum;
import com.av.pixel.enums.ImageRenderOptionEnum;
import com.av.pixel.enums.ImageStyleEnum;
import com.av.pixel.enums.PixelModelEnum;
import com.av.pixel.helper.DateUtil;
import com.av.pixel.response.ideogram.ImageResponse;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;

public class GenerationsMap {

    public static List<GenerationsDTO> toList (List<Generations> generations, TreeSet<String> likedGenerations, Map<String, User> userMap){
        if (CollectionUtils.isEmpty(generations)) {
            return new ArrayList<>();
        }
        return generations.stream()
                .map(g -> {
                    GenerationsDTO genDTO = toGenerationsDTO(g);
                    if (Objects.nonNull(genDTO)) {
                        if (Objects.nonNull(likedGenerations) && likedGenerations.contains(g.getId().toString())){
                            genDTO.setSelfLike(true);
                        }
                        if (Objects.nonNull(userMap) && userMap.containsKey(g.getUserCode())) {
                            User user = userMap.get(g.getUserCode());
                            genDTO.setUserName(user.getFirstName());
                            genDTO.setUserImgUrl(user.getImageUrl());
                        }
                    }
                    return genDTO;
                })
                .toList();
    }

    public static GenerationsDTO toGenerationsDTO(Generations generations){
        if (Objects.isNull(generations)) {
            return null;
        }
        Long likes = (Objects.isNull(generations.getLikes()) || generations.getLikes() < 0) ? 0L : generations.getLikes();
        String creationEpoch = Objects.nonNull(generations.getCreated()) ? DateUtil.formatDate(generations.getCreated()) : "- -";
        PixelModelEnum modelEnum = PixelModelEnum.getModelByName(generations.getModel());
        ImageRenderOptionEnum renderOption = ImageRenderOptionEnum.getEnumByName(generations.getRenderOption());
        ImageStyleEnum styleEnum = ImageStyleEnum.getEnumByName(generations.getStyle());
        return new GenerationsDTO()
                .setGenerationId(generations.getId().toString())
                .setCreationEpoch(creationEpoch)
                .setImages(toPromptImageDTOList(generations.getImages()))
                .setTag(generations.getTag())
                .setCategory(generations.getCategory())
                .setUserCode(generations.getUserCode())
                .setModel(Objects.nonNull(modelEnum) ? modelEnum.getValue() : null)
                .setUserPrompt(generations.getUserPrompt())
                .setLikes(likes)
                .setRenderOption(renderOption.getValue())
                .setSeed(generations.getSeed())
                .setResolution(generations.getResolution())
                .setPrivateImage(generations.getPrivateImage())
                .setStyle(styleEnum.getValue())
                .setColorPalette(generations.getColorPalette())
                .setAspectRatio(generations.getAspectRatio());
    }

    public static List<PromptImageDTO> toPromptImageDTOList (List<PromptImage> promptImages){
        if (CollectionUtils.isEmpty(promptImages)) {
            return new ArrayList<>();
        }
        return promptImages.stream()
                .map(GenerationsMap::toPromptImageDTO)
                .toList();
    }

    public static PromptImageDTO toPromptImageDTO(PromptImage promptImage) {
        if (Objects.isNull(promptImage)) {
            return null;
        }
        return new PromptImageDTO()
                .setImageId(promptImage.getImageId())
                .setMagicPrompt(promptImage.getMagicPrompt())
                .setUrl(promptImage.getUrl())
                .setThumbnail(promptImage.getThumbnail())
                .setSafeImage(promptImage.isSafeImage())
                .setStyle(ImageStyleEnum.getEnumByName(promptImage.getStyle()).getValue());
    }

    public static PromptImage toPromptImage(ImageResponse imageResponse, int index) {
        if (Objects.isNull(imageResponse)) {
            return null;
        }
        return new PromptImage()
                .setImageId(index)
                .setMagicPrompt(imageResponse.getPrompt())
                .setUrl(imageResponse.getUrl())
                .setSafeImage(imageResponse.getIsImageSafe())
                .setStyle(imageResponse.getStyleType())
                .setThumbnail(imageResponse.getThumbnailUrl());
    }

    public static List<PromptImage> toPromptImageList(List<ImageResponse> imageResponse) {
        if (CollectionUtils.isEmpty(imageResponse)) {
            return null;
        }

        int size = imageResponse.size();

        List<PromptImage> promptImages = new ArrayList<>();

        for (int i=0;i<size;i++) {
            promptImages.add(toPromptImage(imageResponse.get(i), i+1));
        }

        return promptImages;
    }

    public static Generations toGenerationsEntity(String userCode, String model, String prompt, String renderOption, Boolean privateImage,
                                                  String style, String colorPalette, AspectRatioEnum aspectRatio, List<ImageResponse> imageResponses){
        return new Generations()
                .setImages(toPromptImageList(imageResponses))
                .setTag(null)
                .setCategory(null)
                .setUserCode(userCode)
                .setModel(model)
                .setUserPrompt(prompt)
                .setLikes(0L)
                .setRenderOption(renderOption)
                .setSeed(getSeed(imageResponses))
                .setResolution(getResolution(imageResponses))
                .setPrivateImage(privateImage)
                .setStyle(style)
                .setColorPalette(colorPalette)
                .setAspectRatio(Objects.isNull(aspectRatio) ? "1:1" : aspectRatio.getValue());
    }

    private static Long getSeed (List<ImageResponse> imageResponses) {
        if (CollectionUtils.isEmpty(imageResponses) || Objects.isNull(imageResponses.get(0))) {
            return 0L;
        }
        return imageResponses.get(0).getSeed();
    }

    private static String getResolution (List<ImageResponse> imageResponses) {
        if (CollectionUtils.isEmpty(imageResponses) || Objects.isNull(imageResponses.get(0))) {
            return null;
        }
        return imageResponses.get(0).getResolution();
    }

}
