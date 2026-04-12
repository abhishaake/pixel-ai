package com.av.pixel.service;

import com.av.pixel.exception.Error;
import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.ses.SesAsyncClient;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.Body;
import software.amazon.awssdk.services.ses.model.Content;
import software.amazon.awssdk.services.ses.model.Destination;
import software.amazon.awssdk.services.ses.model.Message;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;
import software.amazon.awssdk.services.ses.model.SendEmailResponse;
import software.amazon.awssdk.services.ses.model.SesException;

import jakarta.annotation.PostConstruct;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

@Service
@ConditionalOnBean(SesClient.class)
@RequiredArgsConstructor
@Slf4j
public class SesEmailService {

    private static final String UTF_8 = "UTF-8";

    private final SesClient sesClient;
    private final SesAsyncClient sesAsyncClient;

    @Value("${aws.ses.sender-email}")
    private String senderEmail;

    @Value("${app.mail.sender-display-name:}")
    private String senderDisplayName;

    @PostConstruct
    void validateSender() {
        if (StringUtils.isBlank(senderEmail)) {
            throw new IllegalStateException(
                    "aws.ses.sender-email (or SES_SENDER_EMAIL env) is required when aws.ses.enabled is true");
        }
    }

    /**
     * Sends an HTML email via Amazon SES API (not SMTP).
     *
     * @return SES message id for tracing
     */
    public String sendEmail(String to, String subject, String htmlBody) {
        validateInputs(to, subject, htmlBody);
        SendEmailRequest request = buildSendRequest(to, subject, htmlBody);
        try {
            SendEmailResponse response = sesClient.sendEmail(request);
            String messageId = response.messageId();
            log.info("SES email accepted: to={}, response={}", to, response);
            return messageId;
        } catch (SesException e) {
            log.error(
                    "SES SendEmail failed: status={}, errorCode={}, requestId={}, message={}",
                    e.statusCode(),
                    e.awsErrorDetails() != null ? e.awsErrorDetails().errorCode() : null,
                    e.requestId(),
                    e.getMessage());
            throw new Error(
                    HttpStatus.BAD_GATEWAY,
                    "Email could not be sent. Please try again later.");
        } catch (SdkClientException e) {
            log.error("SES client error while sending to={}", to, e);
            throw new Error(
                    HttpStatus.BAD_GATEWAY,
                    "Email could not be sent. Please try again later.");
        }
    }

    /**
     * Sends an HTML email asynchronously using {@link SesAsyncClient}.
     *
     * @return future completing with the SES message id, or failing with {@link Error}
     */
    public CompletableFuture<String> sendEmailAsync(String to, String subject, String htmlBody) {
        validateInputs(to, subject, htmlBody);
        SendEmailRequest request = buildSendRequest(to, subject, htmlBody);
        return sesAsyncClient
                .sendEmail(request)
                .thenApply(response -> {
                    log.info("SES async email accepted: to={}, messageId={}", to, response.messageId());
                    return response.messageId();
                })
                .exceptionally(ex -> {
                    Throwable cause = ex instanceof CompletionException && ex.getCause() != null
                            ? ex.getCause()
                            : ex;
                    if (cause instanceof SesException sesException) {
                        log.error(
                                "SES async SendEmail failed: status={}, errorCode={}, requestId={}, message={}",
                                sesException.statusCode(),
                                sesException.awsErrorDetails() != null
                                        ? sesException.awsErrorDetails().errorCode()
                                        : null,
                                sesException.requestId(),
                                sesException.getMessage());
                    } else {
                        log.error("SES async client error while sending to={}", to, cause);
                    }
                    throw new Error(
                            HttpStatus.BAD_GATEWAY,
                            "Email could not be sent. Please try again later.");
                });
    }

    private SendEmailRequest buildSendRequest(String to, String subject, String htmlBody) {
        return SendEmailRequest.builder()
                .source(formatSesSource())
                .destination(Destination.builder().toAddresses(to.trim()).build())
                .message(Message.builder()
                        .subject(Content.builder().charset(UTF_8).data(subject).build())
                        .body(Body.builder()
                                .html(Content.builder().charset(UTF_8).data(htmlBody).build())
                                .build())
                        .build())
                .build();
    }

    /**
     * SES accepts {@code Display Name <verified@domain>} when the mailbox part matches a verified identity.
     */
    private String formatSesSource() {
        String email = senderEmail.trim();
        if (StringUtils.isBlank(senderDisplayName)) {
            return email;
        }
        return senderDisplayName.trim() + " <" + email + ">";
    }

    private static void validateInputs(String to, String subject, String htmlBody) {
        if (StringUtils.isBlank(to)) {
            throw new Error(HttpStatus.BAD_REQUEST, "Recipient address is required");
        }
        if (StringUtils.isBlank(subject)) {
            throw new Error(HttpStatus.BAD_REQUEST, "Subject is required");
        }
        if (StringUtils.isBlank(htmlBody)) {
            throw new Error(HttpStatus.BAD_REQUEST, "HTML body is required");
        }
    }
}
