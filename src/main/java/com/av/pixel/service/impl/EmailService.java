package com.av.pixel.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender javaMailSender;

    @Value("${spring.mail.username}")
    private String sender;

    @Value("${spring.mail.receiver}")
    private String receiver;

    @Async
    public void sendErrorMail (String body) {
        List<String> recipients = List.of(receiver.split(","));

        for(String rec : recipients) {
            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setFrom(sender);
            mailMessage.setTo(rec);
            mailMessage.setText(body);
            mailMessage.setSubject("Pixel Exception");
            sendSimpleMail(mailMessage);
        }
    }

    @Async
    public void sendPaymentErrorMail (String message, String body) {
        List<String> recipients = List.of(receiver.split(","));

        for(String rec : recipients) {
            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setFrom(sender);
            mailMessage.setTo(rec);
            mailMessage.setText(body);
            mailMessage.setSubject("Pixel Payment Exception");
            sendSimpleMail(mailMessage);
        }
    }

    @Async
    public void sendPaymentMail (String message, String body) {
        List<String> recipients = List.of(receiver.split(","));

        for(String rec : recipients) {
            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setFrom(sender);
            mailMessage.setTo(rec);
            mailMessage.setText(body);
            mailMessage.setSubject("Pixel Payment");
            sendSimpleMail(mailMessage);
        }
    }

    public String sendSimpleMail (SimpleMailMessage simpleMailMessage) {
        try {
            javaMailSender.send(simpleMailMessage);
            return "Mail Sent Successfully";
        }
        catch (Exception e) {
            log.error("send email error", e);
        }
        return null;
    }
}
