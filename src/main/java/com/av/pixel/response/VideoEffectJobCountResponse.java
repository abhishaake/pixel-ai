package com.av.pixel.response;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class VideoEffectJobCountResponse {

    long count;
}
