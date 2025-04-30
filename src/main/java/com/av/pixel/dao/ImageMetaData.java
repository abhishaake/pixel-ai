package com.av.pixel.dao;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class ImageMetaData {
    Long seed;
    String resolution;
}
