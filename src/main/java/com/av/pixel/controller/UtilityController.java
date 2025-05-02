package com.av.pixel.controller;

import com.av.pixel.exception.Error;
import com.av.pixel.helper.DateUtil;
import com.av.pixel.response.base.Response;
import com.av.pixel.response.ideogram.ImageResponse;
import com.av.pixel.scheduler.CacheScheduler;
import com.av.pixel.scheduler.PaymentScheduler;
import com.av.pixel.service.AdminConfigService;
import com.av.pixel.service.ImageCompressionService;
import com.av.pixel.service.ModelPricingService;
import com.av.pixel.service.S3Service;
import com.av.pixel.service.impl.GenerationsServiceImpl;
import com.av.pixel.service.impl.ImageCompressionServiceImpl;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.http.HttpResponse;

import static com.av.pixel.mapper.ResponseMapper.response;

@RestController
@Slf4j
@RequestMapping("/api/v1/utility")
@AllArgsConstructor
public class UtilityController {

    CacheScheduler cacheScheduler;

    S3Service s3Service;

    PaymentScheduler paymentScheduler;

    AdminConfigService adminConfigService;

    GenerationsServiceImpl generationsService;

    ImageCompressionServiceImpl imageCompressionService;

    @GetMapping("/health")
    public Response<String> health() {
        return new Response<>(HttpStatus.OK, "healthy");
    }

    @PostMapping("/load-pricing-cache")
    public Response<String> loadPricingCache() {
        cacheScheduler.loadModelPricing();
        return new Response<>(HttpStatus.OK, "success");
    }

    @PostMapping("/s3/upload")
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            String filename = file.getOriginalFilename();
            String fileUrl = s3Service.uploadFile(filename, file.getBytes());
            return ResponseEntity.ok(fileUrl);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to upload file");
        }
    }

    @GetMapping("/s3/file/download")
    public ResponseEntity<byte[]> downloadFile(@RequestParam String fileName) {
        byte[] data = s3Service.downloadFile(fileName);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDispositionFormData("attachment", fileName);
        return new ResponseEntity<>(data, headers, HttpStatus.OK);
    }

    @GetMapping("/s3/file/url")
    public ResponseEntity<Response<String>> getFileUrl(@RequestParam String fileName) {
        String url = s3Service.getPublicUrl(fileName);
        return response(url, HttpStatus.OK);
    }

    @PostMapping("/s3/upload-from-url")
    public ResponseEntity<String> uploadFromUrl(
            @RequestParam("imageUrl") String imageUrl,
            @RequestParam("fileName") String fileName) {
        try {
            if (!fileName.contains(".png") && !fileName.contains(".jpg") && !fileName.contains(".jpeg")) {
                throw new Error("Only png, jpg and jpeg allowed");
            }
            String fileUrl = s3Service.downloadImageAndUploadToS3(imageUrl, fileName);
            return ResponseEntity.ok(fileUrl);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to upload image from URL: " + e.getMessage());
        }
    }

    @PostMapping("/scheduler/failed-payments")
    public Response<String> handleFailedPayments () {
        paymentScheduler.handleFailedPayments();
        return new Response<>(HttpStatus.OK, "success");
    }

    @PostMapping("/load-admin-config")
    public Response<String> loadAdminConfig () {
        adminConfigService.loadAdminConfig();
        return new Response<>(HttpStatus.OK, "success");
    }

    @PostMapping("/upload")
    public Response<ImageResponse> upload (@RequestBody ImageResponse imageResponse, @RequestParam String userCode) {
        return new Response<>(generationsService.uploadToS3(imageResponse, userCode, DateUtil.currentTimeMillis(), 0));
    }

    @GetMapping("/compression-test")
    public ResponseEntity<byte[]> compressionTest (@RequestParam String url,
                                             @RequestParam float scale,
                                             @RequestParam float quality) throws IOException {
        HttpResponse<byte[]> res = s3Service.downloadImage(url);
        HttpHeaders headers = new HttpHeaders();
        Long ep = DateUtil.currentTimeMillis();
        headers.setContentDispositionFormData("attachment", ep + ".png");
        return new ResponseEntity<>(imageCompressionService.test(res.body(), scale, quality), headers, HttpStatus.OK);
    }
}
