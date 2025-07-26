package com.av.pixel.controller;

import com.apple.itunes.storekit.client.APIException;
import com.av.pixel.apple.AppStoreService;
import com.av.pixel.dao.Generations;
import com.av.pixel.enums.ImageCompressionConfig;
import com.av.pixel.exception.Error;
import com.av.pixel.helper.DateUtil;
import com.av.pixel.repository.GenerationsRepository;
import com.av.pixel.response.base.Response;
import com.av.pixel.response.ideogram.ImageResponse;
import com.av.pixel.scheduler.CacheScheduler;
import com.av.pixel.scheduler.ImageScheduler;
import com.av.pixel.scheduler.PaymentScheduler;
import com.av.pixel.service.AdminConfigService;
import com.av.pixel.service.S3Service;
import com.av.pixel.service.impl.EmailService;
import com.av.pixel.service.impl.GenerationsServiceImpl;
import com.av.pixel.service.impl.ImageCompressionServiceImpl;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

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

    GenerationsRepository generationsRepository;

    EmailService emailService;

    ImageScheduler imageScheduler;

    AppStoreService appStoreService;

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

    @PostMapping("/add-gen")
    public Response<String> addGen (@RequestBody Generations generations) {
        generations.setEpoch(DateUtil.currentTimeSec());
        generations.setLikes(0l);
        generations.setViews(1000L);
        generations = generationsRepository.save(generations);
        log.info("saved : {}", generations.getId().toString());
        return new Response<>("success");
    }

    @PostMapping("/uploadAndGet")
    public Response<?> getImgeUrl (@RequestParam("file") MultipartFile file) {
        try {
            Long epoch = DateUtil.currentTimeSec();
            String fileName = "P108" + "_" + epoch + ".png";
            String fileUrl = s3Service.uploadFile(fileName, file.getBytes());
            Map<String, String> map = new HashMap<>();
            map.put("url", fileUrl);
            double imageSize = imageCompressionService.getImageSize(file.getBytes());
            log.info("img size : {}", imageSize);
            if (imageCompressionService.isCompressionRequired(imageSize)) {
                ImageCompressionConfig config = imageCompressionService.getRequiredCompression(imageSize);
                if (Objects.isNull(config)) {
                    return new Response<>(map);
                } else {
                    byte[] compressedImage = imageCompressionService.getCompressedImage(file.getBytes(), config);
                    String thumbnailUrl = s3Service.uploadFile("P108" + "_" + epoch + "_thumbnail.png", compressedImage);
                    map.put("url2", thumbnailUrl);
                }
            }
            return new Response<>(map);
        } catch (IOException e) {
            return new Response<>(null);
        }
    }

    @PostMapping("/send-mail")
    public Response<?> sendMail (@RequestParam String body) {
        emailService.sendErrorMail(body);
        return new Response<>("success");
    }

    @GetMapping("/view-scheduler")
    public Response<?> viewScheduler () {
        imageScheduler.resetViews();
        return new Response<>("success");
    }

    @PostMapping("/temp")
    public Response<?> temp () {
        imageScheduler.tempMethod();
        return new Response<>("success");
    }

    @GetMapping("/apple/verify/{txnId}")
    public Response<?> verifyAppleTxn(@PathVariable("txnId") String txnId) throws APIException, IOException {
        return new Response<>(appStoreService.verifyTransactionWithOfficalClient(txnId));
    }
}
