package com.av.pixel.request;

import com.av.pixel.enums.ImageActionEnum;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class ImageActionRequest {

    String generationId;
    ImageActionEnum action;
}
