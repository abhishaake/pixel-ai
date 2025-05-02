package com.av.pixel.dao;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class PromptImage {
    int imageId;
    String url;
    String thumbnail;
    String magicPrompt;
    boolean safeImage;
    String style;
}
