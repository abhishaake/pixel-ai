package com.av.pixel.controller;

import com.av.pixel.service.EmailTrackingService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/track")
@RequiredArgsConstructor
public class EmailTrackingController {

    private final EmailTrackingService emailTrackingService;

    @GetMapping("/click")
    public ResponseEntity<Void> click(
            @RequestParam String uid,
            @RequestParam String cid,
            @RequestParam String redirect,
            @RequestParam(required = false) String platform,
            HttpServletRequest request) {
        URI location = emailTrackingService.trackClickAndResolveRedirect(uid, cid, redirect, platform, request);
        return ResponseEntity.status(HttpStatus.FOUND).location(location).build();
    }

    @GetMapping("/open")
    public ResponseEntity<byte[]> open(
            @RequestParam String uid,
            @RequestParam String cid,
            HttpServletRequest request) {
        emailTrackingService.trackOpen(uid, cid, request);
        byte[] body = emailTrackingService.transparentTrackingGif();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_GIF);
        headers.add(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate");
        return new ResponseEntity<>(body, headers, HttpStatus.OK);
    }
}
