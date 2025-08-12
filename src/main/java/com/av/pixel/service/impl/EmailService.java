package com.av.pixel.service.impl;

import com.av.pixel.dao.User;
import com.av.pixel.dto.UserDTO;
import com.av.pixel.repository.GenerationHistoryRepository;
import com.av.pixel.repository.UserRepository;
import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender javaMailSender;
    private final UserRepository userRepository;
    private final GenerationHistoryRepository generationHistoryRepository;

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

    @Async
    public void sendMilestoneMail(String userCode) {
        long count = generationHistoryRepository.countByUserCode(userCode);
        if (count != 1 && (count % 20 != 0)) {
            return;
        }
        List<String> recipients = List.of(receiver.split(","));
        String body = " Milestone :: " + userCode + " credits utilised : " + count;

        for (String rec : recipients) {
            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setFrom(sender);
            mailMessage.setTo(rec);
            mailMessage.setText(body);
            mailMessage.setSubject("Pixel : Milestone : " + userCode);
            sendSimpleMail(mailMessage);
        }
    }

    @Async
    public void sendPaymentMail (String message, String body, String userCode) {
        List<String> recipients = List.of(receiver.split(","));

        User user = userRepository.findByCodeAndDeletedFalse(userCode);
        String extBody = "";
        String header = "";

        if (Objects.nonNull(user)) {
            extBody = "\n" +
                    "user details : " + "\n" +
                    "name : " + user.getFirstName() + " " + user.getLastName() + "\n" +
                    "email : " + user.getEmail() + "\n" +
                    "code : " + userCode;
            header = " by " + userCode;
        }

        for(String rec : recipients) {
            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setFrom(sender);
            mailMessage.setTo(rec);
            mailMessage.setText(body + extBody);
            mailMessage.setSubject("Pixel Payment" + header);
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
