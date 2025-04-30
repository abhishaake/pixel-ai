package com.av.pixel.controller;

import com.av.pixel.service.CallbackService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
@Slf4j
@RequestMapping("/api/v1/callback")
public class CallbackController {

    CallbackService callbackService;

    @GetMapping("/ad")
    public void handleAdCallback (HttpServletRequest servletRequest) {
        callbackService.handleAdCallback(servletRequest);
    }

    @PostMapping("/ad")
    public void handleAdCallbackPost (HttpServletRequest servletRequest) {
        callbackService.handleAdCallback(servletRequest);
    }
}
