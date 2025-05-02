package com.av.pixel.service;

import java.net.http.HttpResponse;
import java.util.List;

public interface S3Service {
    String getPublicUrl(String objectKey);

    String downloadImageAndUploadToS3(String imageUrl, String fileName);

    String uploadFile(String fileName, byte[] fileContent);

    byte[] downloadFile(String fileName);

    String updateFile(String fileName, byte[] fileContent);

    void deleteFile(String fileName);

    List<String> listFiles();

    HttpResponse<byte[]> downloadImage (String imageUrl);

    String uploadToS3 (byte[] imageByte, String fileName);

    String getImageExtension (String fileName, HttpResponse<byte[]> response);

    String getImageExtensionName (HttpResponse<byte[]> response);
}
