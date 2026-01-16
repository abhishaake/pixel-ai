package com.av.pixel.service.impl;

import com.av.pixel.enums.ImageCompressionConfig;
import com.av.pixel.service.ImageCompressionService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Objects;

@Service
@Slf4j
@AllArgsConstructor
public class ImageCompressionServiceImpl implements ImageCompressionService {

    private static final double KB = 1024;
    private static final double SIZE_THRESHOLD_KB = 900;

    @Override
    public double getImageSize (byte[] imageBytes) {
        return (double) imageBytes.length / KB;
    }

    @Override
    public boolean isCompressionRequired (double size) {
        return size > SIZE_THRESHOLD_KB;
    }

    @Override
    public ImageCompressionConfig getRequiredCompression (double size) {
        return ImageCompressionConfig.getBySize(size);
    }

    @Override
    public byte[] getCompressedImage (byte[] imageBytes, ImageCompressionConfig config) {
        if (Objects.isNull(config)) {
            return imageBytes;
        }
        float quality = config.getQuality();
        float scale = config.getScale();

        try {
            return compressImage(imageBytes, scale, quality);
        }
        catch (Exception e){
            log.error("getCompressedImage error", e);
            return imageBytes;
        }
    }


    private byte[] compressImage(byte[] imageBytes, float scale, float quality) throws IOException {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(imageBytes);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        String ext = "PNG";

        Thumbnails.of(inputStream)
                .scale(scale)
                .outputQuality(quality)
                .outputFormat(ext)
                .toOutputStream(outputStream);

        return outputStream.toByteArray();
    }


    public byte[] test (byte[] imageBytes, float scale, float quality) throws IOException {
        double imageSize = getImageSize(imageBytes);
        byte[] compressedImage = compressImage(imageBytes, scale, quality);
        double newImageSize = getImageSize(compressedImage);

        log.info("original size: {}, compressed size: {}, scale :{} , quality: {}", imageSize, newImageSize, scale, quality);
        return compressedImage;
    }
}
