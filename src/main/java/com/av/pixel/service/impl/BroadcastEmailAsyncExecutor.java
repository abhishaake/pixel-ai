package com.av.pixel.service.impl;

import com.av.pixel.dao.User;
import com.av.pixel.enums.EmailTemplateVariable;
import com.av.pixel.enums.SendToEnum;
import com.av.pixel.repository.UserRepository;
import com.av.pixel.request.BroadcastEmailRequest;
import io.micrometer.common.util.StringUtils;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class BroadcastEmailAsyncExecutor {

    private static final int BATCH_SIZE = 50;
    private static final long BATCH_DELAY_MS = 5000L;
    private static final String DEFAULT_SUBJECT = "Pixel notification";

    private final JavaMailSender javaMailSender;
    private final UserRepository userRepository;

    @Value("${spring.mail.username}")
    private String sender;

    @Async
    public void execute(BroadcastEmailRequest request) {
        String subject = resolveSubject(request.getSubject());
        if (SendToEnum.USER.equals(request.getSendTo())) {
            sendToSingleUser(request, subject);
            return;
        }
        sendToAllUsers(request, subject);
    }

    private void sendToSingleUser(BroadcastEmailRequest request, String subject) {
        User user = userRepository.findByEmailAndDeletedFalse(request.getEmail());
        String body = applyVariables(request.getHtml(), request.getVariableNames(), user);
        sendHtml(request.getEmail(), subject, body);
    }

    private void sendToAllUsers(BroadcastEmailRequest request, String subject) {
        int pageNumber = 0;
        Page<User> page;
        do {
            page = userRepository.findAllByDeletedFalse(PageRequest.of(pageNumber, BATCH_SIZE));
            for (User user : page.getContent()) {
                if (StringUtils.isEmpty(user.getEmail())) {
                    continue;
                }
                String body = applyVariables(request.getHtml(), request.getVariableNames(), user);
                sendHtml(user.getEmail(), subject, body);
            }
            if (page.hasNext()) {
                pauseBetweenBatches();
            }
            pageNumber++;
        } while (page.hasNext());
    }

    private void pauseBetweenBatches() {
        try {
            Thread.sleep(BATCH_DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Broadcast email batch delay interrupted");
        }
    }

    private String resolveSubject(String subject) {
        return StringUtils.isEmpty(subject) ? DEFAULT_SUBJECT : subject.trim();
    }

    private String applyVariables(String html, List<String> variableNames, User user) {
        if (StringUtils.isEmpty(html) || CollectionUtils.isEmpty(variableNames)) {
            return html;
        }
        String result = html;
        for (String name : variableNames) {
            EmailTemplateVariable variable = EmailTemplateVariable.fromName(name);
            String value = variable.resolve(user);
            result = result.replace("{{" + variable.name() + "}}", value);
        }
        return result;
    }

    private void sendHtml(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(sender);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            javaMailSender.send(message);
        } catch (Exception e) {
            log.error("Failed to send HTML email to {}", to, e);
        }
    }
}
