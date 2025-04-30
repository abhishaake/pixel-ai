package com.av.pixel.service.impl;

import com.av.pixel.service.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class S3ServiceImpl implements S3Service {

    final S3Client s3Client;


    @Value("${aws.s3.bucket}")
    private String bucketName;

    @Value("${aws.region}")
    private String region;

    public String getPublicUrl(String objectKey) {
        return String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, objectKey);
    }

    public String downloadImageAndUploadToS3(String imageUrl, String fileName) {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(imageUrl))
                    .GET()
                    .build();

            HttpResponse<byte[]> response = client.send(
                    request,
                    HttpResponse.BodyHandlers.ofByteArray()
            );

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                byte[] fileContent = response.body();
                fileName = getImageExtension(fileName, response);
                return uploadFile(fileName, fileContent);
            } else {
                throw new RuntimeException("Failed to download image, status code: " + response.statusCode());
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to download and upload image", e);
        }
    }

    private String getImageExtension (String fileName, HttpResponse<byte[]> response) {
        if (fileName.contains(".png") || fileName.contains(".jpg") || fileName.contains(".jpeg")) {
            return fileName;
        }
        String contentType = response.headers().firstValue("Content-Type").orElse("application/octet-stream");
        String extension;
        if (contentType.equalsIgnoreCase("image/png")) {
            extension = ".png";
        } else if (contentType.equalsIgnoreCase("image/jpeg") || contentType.equalsIgnoreCase("image/jpg")) {
            extension = ".jpg";
        } else {
            extension = ".png";
        }
        return fileName + extension;
    }


    public String uploadFile(String fileName, byte[] fileContent) {
        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .build();

            PutObjectResponse res = s3Client.putObject(putObjectRequest, RequestBody.fromBytes(fileContent));
            return getPublicUrl(fileName);
        } catch (S3Exception e) {
            throw new RuntimeException("Failed to upload file to S3", e);
        }
    }

    public byte[] downloadFile(String fileName) {
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .build();

            ResponseBytes<GetObjectResponse> objectBytes = s3Client.getObjectAsBytes(getObjectRequest);
            return objectBytes.asByteArray();
        } catch (S3Exception e) {
            throw new RuntimeException("Failed to download file from S3", e);
        }
    }

    public String updateFile(String fileName, byte[] fileContent) {
        return uploadFile(fileName, fileContent);
    }

    public void deleteFile(String fileName) {
        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
        } catch (S3Exception e) {
            throw new RuntimeException("Failed to delete file from S3", e);
        }
    }

    public List<String> listFiles() {
        try {
            ListObjectsV2Request listObjectsRequest = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .build();

            ListObjectsV2Response response = s3Client.listObjectsV2(listObjectsRequest);
            return response.contents().stream()
                    .map(S3Object::key)
                    .collect(Collectors.toList());
        } catch (S3Exception e) {
            throw new RuntimeException("Failed to list files from S3", e);
        }
    }
}
