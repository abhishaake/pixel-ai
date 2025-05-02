package com.av.pixel.dto;

import com.av.pixel.dao.ImageMetaData;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class PromptImageDTO {
    int imageId;
    String url;
    String magicPrompt;
    boolean safeImage;
    String style;
}
