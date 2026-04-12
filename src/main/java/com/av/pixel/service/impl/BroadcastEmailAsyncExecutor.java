package com.av.pixel.service.impl;

import com.av.pixel.dao.User;
import com.av.pixel.enums.EmailTemplateVariable;
import com.av.pixel.enums.SendToEnum;
import com.av.pixel.repository.UserRepository;
import com.av.pixel.request.BroadcastEmailRequest;
import com.av.pixel.service.SesEmailService;
import io.micrometer.common.util.StringUtils;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Component
@RequiredArgsConstructor
@Slf4j
public class BroadcastEmailAsyncExecutor {

    private static final int BATCH_SIZE = 50;
    private static final long BATCH_DELAY_MS = 5000L;
    private static final String DEFAULT_SUBJECT = "Pixel notification";

    private final JavaMailSender javaMailSender;
    private final UserRepository userRepository;
    private final ObjectProvider<SesEmailService> sesEmailServiceProvider;

    @Value("${spring.mail.username}")
    private String sender;

    @Value("${app.mail.sender-display-name:}")
    private String senderDisplayName;

    @Async
    public void execute(BroadcastEmailRequest request) {
        if (SendToEnum.USER.equals(request.getSendTo())) {
            sendToSingleUser(request);
            return;
        }
        sendToAllUsers(request);
    }

    private void sendToSingleUser(BroadcastEmailRequest request) {
        User user = userRepository.findByEmailAndDeletedFalse(request.getEmail());
        String subject = pickSubject(request);
        String body = applyVariables(request.getHtml(), request.getVariableNames(), user, "sub0_");
        sendHtml(request.getEmail(), subject, body);
    }

    private void sendToAllUsers(BroadcastEmailRequest request) {
        int pageNumber = 0;
        Page<User> page;
        List<String> subjectCandidates = getSubjectCandidates(request);
        do {
            page = userRepository.findAllByDeletedFalse(PageRequest.of(pageNumber, BATCH_SIZE));
            for (User user : page.getContent()) {
                if (StringUtils.isEmpty(user.getEmail())) {
                    continue;
                }
                int subjectIdx = getSubjectIdx(subjectCandidates);
                String subject = subjectCandidates.get(subjectIdx);
                String subPrefix = "sub" + subjectIdx + "_";
                String body = applyVariables(request.getHtml(), request.getVariableNames(), user, subPrefix);
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

    private int getSubjectIdx(List<String> candidates) {
        return ThreadLocalRandom.current().nextInt(candidates.size());
    }

    private List<String> getSubjectCandidates(BroadcastEmailRequest request) {
        List<String> subjects = request.getSubjects();
        List<String> candidates = new ArrayList<>();
        if (!CollectionUtils.isEmpty(subjects)) {
            for (String s : subjects) {
                if (StringUtils.isNotEmpty(s)) {
                    String t = s.trim();
                    if (StringUtils.isNotEmpty(t)) {
                        candidates.add(t);
                    }
                }
            }
        }
        return candidates;
    }

    private String pickSubject(BroadcastEmailRequest request) {
        List<String> subjects = request.getSubjects();
        if (!CollectionUtils.isEmpty(subjects)) {
            List<String> candidates = new ArrayList<>();
            for (String s : subjects) {
                if (StringUtils.isNotEmpty(s)) {
                    String t = s.trim();
                    if (StringUtils.isNotEmpty(t)) {
                        candidates.add(t);
                    }
                }
            }
            if (!candidates.isEmpty()) {
                return candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
            }
        }
        return resolveSubject(request.getSubject());
    }

    private String resolveSubject(String subject) {
        return StringUtils.isEmpty(subject) ? DEFAULT_SUBJECT : subject.trim();
    }

    private String applyVariables(String html, List<String> variableNames, User user, String subPrefix) {
        if (StringUtils.isEmpty(html) || CollectionUtils.isEmpty(variableNames)) {
            return html;
        }
        String result = html;
        String emailIdentifier = null;
        for (String name : variableNames) {
            EmailTemplateVariable variable = EmailTemplateVariable.fromName(name);
            String value;
            if (variable == EmailTemplateVariable.EMAIL_IDENTIFIER) {
                if (emailIdentifier == null) {
                    emailIdentifier = UUID.randomUUID().toString();
                    emailIdentifier = subPrefix + emailIdentifier;
                }
                value = emailIdentifier;
            } else {
                value = variable.resolve(user);
            }
            result = result.replace("{{" + variable.name() + "}}", value);
        }
        return result;
    }

    private void sendHtml(String to, String subject, String htmlBody) {
        SesEmailService sesEmailService = sesEmailServiceProvider.getIfAvailable();
        if (sesEmailService != null) {
            try {
                sesEmailService.sendEmail(to, subject, htmlBody);
            } catch (Exception e) {
                log.error("Failed to send broadcast HTML email via Amazon SES to {}", to, e);
            }
            return;
        }
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            if (StringUtils.isNotEmpty(senderDisplayName)) {
                helper.setFrom(sender, senderDisplayName.trim());
            } else {
                helper.setFrom(sender);
            }
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            javaMailSender.send(message);
        } catch (Exception e) {
            log.error("Failed to send HTML email to {}", to, e);
        }
    }
}
