package com.av.pixel.controller;

import com.av.pixel.request.SesSampleEmailRequest;
import com.av.pixel.response.SesSampleEmailResponse;
import com.av.pixel.service.SesEmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Example REST entry point for {@link SesEmailService}. Active only when profile is {@code dev} or
 * {@code local} and SES beans are enabled ({@code aws.ses.enabled=true}).
 */
@RestController
@Profile({"dev", "local"})
@ConditionalOnBean(SesEmailService.class)
@RequestMapping("/api/v1/dev/ses")
@RequiredArgsConstructor
@Slf4j
public class SesEmailSampleController {

    private final SesEmailService sesEmailService;

    @PostMapping("/send-sample")
    public ResponseEntity<SesSampleEmailResponse> sendSample(@RequestBody SesSampleEmailRequest body) {
        String messageId = sesEmailService.sendEmail(body.getTo(), body.getSubject(), body.getHtmlBody());
        return ResponseEntity.ok(new SesSampleEmailResponse(messageId));
    }
}
