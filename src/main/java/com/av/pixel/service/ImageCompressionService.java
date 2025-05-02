package com.av.pixel.service;

import com.av.pixel.enums.ImageCompressionConfig;

public interface ImageCompressionService {

    double getImageSize (byte[] imageBytes);

    boolean isCompressionRequired (double size);

    ImageCompressionConfig getRequiredCompression (double size);

    byte[] getCompressedImage (byte[] imageBytes, ImageCompressionConfig config);
}
