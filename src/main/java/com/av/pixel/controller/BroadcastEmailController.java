package com.av.pixel.controller;

import com.av.pixel.request.BroadcastEmailRequest;
import com.av.pixel.response.base.Response;
import com.av.pixel.service.BroadcastEmailService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/email")
@AllArgsConstructor
public class BroadcastEmailController {

    private final BroadcastEmailService broadcastEmailService;

    @PostMapping("/broadcast")
    public ResponseEntity<Response<String>> broadcast(@RequestBody BroadcastEmailRequest request) {
        broadcastEmailService.queueBroadcast(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(new Response<>(HttpStatus.ACCEPTED, "queued"));
    }
}
