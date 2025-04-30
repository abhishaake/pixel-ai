package com.av.pixel.service;

import java.util.List;

public interface S3Service {
    String getPublicUrl(String objectKey);

    String downloadImageAndUploadToS3(String imageUrl, String fileName);

    String uploadFile(String fileName, byte[] fileContent);

    byte[] downloadFile(String fileName);

    String updateFile(String fileName, byte[] fileContent);

    void deleteFile(String fileName);

    List<String> listFiles();
}
