package com.av.pixel.service.impl;

import com.av.pixel.dao.Generations;
import com.av.pixel.dao.NotificationHistory;
import com.av.pixel.dao.NotificationTemplate;
import com.av.pixel.dao.User;
import com.av.pixel.dto.NotificationDTO;
import com.av.pixel.helper.DateUtil;
import com.av.pixel.helper.GenerationHelper;
import com.av.pixel.repository.NotificationHistoryRepository;
import com.av.pixel.repository.NotificationTemplatesRepository;
import com.av.pixel.repository.UserRepository;
import com.av.pixel.response.NotificationResponse;
import com.av.pixel.service.NotificationService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
@AllArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationHistoryRepository notificationHistoryRepository;
    private final NotificationTemplatesRepository notificationTemplatesRepository;
    private final GenerationHelper generationHelper;
    private final UserRepository userRepository;

    @Override
    public void addWelcomeNotification (String userCode, Integer tokens) {
        String welcomeType = "NEW_JOIN_WELCOME";
        String welcomeCredits = "WELCOME_CREDITS";
        String welcomePolicy = "WELCOME_POLICY";

        List<NotificationTemplate> welcomeTemplates  = notificationTemplatesRepository.findByTypeIn(List.of(welcomeType, welcomeCredits, welcomePolicy));
        Optional<NotificationTemplate> newJoinTemplate = welcomeTemplates.stream().filter(t->welcomeType.equals(t.getType())).findFirst();
        Optional<NotificationTemplate> creditTemplate = welcomeTemplates.stream().filter(t->welcomeCredits.equals(t.getType())).findFirst();
        Optional<NotificationTemplate> policyTemplate = welcomeTemplates.stream().filter(t->welcomePolicy.equals(t.getType())).findFirst();

        NotificationDTO notificationDTO1 = new NotificationDTO(getWelcomeNotification(newJoinTemplate.get().getTemplate()));
        List<NotificationDTO> notifications = new java.util.ArrayList<>(List.of(notificationDTO1));
        if (Objects.nonNull(tokens) && tokens > 0) {
            NotificationDTO notificationDTO2 = new NotificationDTO(getCreditNotification(creditTemplate.get().getTemplate(), tokens));
            notifications.add(notificationDTO2);
        }
        notifications.add(new NotificationDTO(getWelcomeNotification(policyTemplate.get().getTemplate())));
        createOrUpdateNotification(userCode, notifications);
    }

    private String getWelcomeNotification (String template) {
        return template.replaceAll("<<dateTime>>", getFormattedDate());
    }

    private String getCreditNotification (String template, Integer tokens) {
        if (Objects.isNull(tokens) || tokens==0) {
            return null;
        }
        template = template.replaceAll("<<tokens>>", String.valueOf(tokens));
        template = template.replaceAll("<<dateTime>>", getFormattedDate());
        return template;
    }

    private String getFormattedDate () {
        return DateUtil.formatDateTime(new Date());
    }

    @Override
    public void addNewTokenNotification (String userCode, Integer tokens) {
        String creditsAdded = "CREDITS_ADDED";

        NotificationTemplate notificationTemplate = notificationTemplatesRepository.findByType(creditsAdded);

        NotificationDTO notificationDTO = new NotificationDTO(getCreditNotification(notificationTemplate.getTemplate(), tokens));

        createOrUpdateNotification(userCode, List.of(notificationDTO));
    }

    @Override
    public void addLikeNotification (String userCode, String likedBy, String imgUrl) {
        String likeTemplate = "NEW_LIKE";

        NotificationTemplate notificationTemplate = notificationTemplatesRepository.findByType(likeTemplate);

        NotificationDTO notificationDTO = new NotificationDTO(getLikedByNotification(notificationTemplate.getTemplate(), likedBy, imgUrl));

        createOrUpdateNotification(userCode, List.of(notificationDTO));
    }

    private void createOrUpdateNotification (String userCode, List<NotificationDTO> notificationDTOs) {
        NotificationHistory notificationHistory = notificationHistoryRepository.findByUserCodeAndDeletedFalse(userCode);

        if (Objects.isNull(notificationHistory)) {
            notificationHistory = new NotificationHistory()
                    .setUserCode(userCode);
        }

        if (CollectionUtils.isEmpty(notificationHistory.getNotifications())) {
            notificationHistory.setNotifications(notificationDTOs);
        } else {
            notificationHistory.getNotifications().addAll(notificationDTOs);
        }

        notificationHistoryRepository.save(notificationHistory);
    }

    private String getLikedByNotification (String template, String likedBy, String imgUrl) {
        template = template.replaceAll("<<likedBy>>", likedBy);
        template = template.replaceAll("<<imgUrl>>", imgUrl);
        return template.replaceAll("<<dateTime>>", getFormattedDate());
    }

    @Override
    @Async
    public void sendLikeNotification (String genId, String likedBy) {
        Generations generation = generationHelper.getById(genId);
        String userCode = generation.getUserCode();
        String imgUrl = generation.getImages().get(0).getThumbnail();
        User user = userRepository.findByCodeAndDeletedFalse(likedBy);
        if (Objects.nonNull(user)) {
            addLikeNotification(userCode, user.getFirstName(), imgUrl);
        }
    }

    @Override
    public NotificationResponse getUserNotifications (String userCode) {
        NotificationHistory notificationHistory = notificationHistoryRepository.findByUserCodeAndDeletedFalse(userCode);
        NotificationResponse notificationResponse = new NotificationResponse();
        if (Objects.isNull(notificationHistory)) {
            return notificationResponse;
        }
        return notificationResponse.setNotifications(notificationHistory.getNotifications()
                .stream().map(NotificationDTO::getContent)
                .collect(Collectors.toList()));
    }

    @Override
    public void sendPaymentSuccessNotification (String userCode, Integer tokens) {
        try {
            String likeTemplate = "PAYMENT_SUCCESS";

            NotificationTemplate notificationTemplate = notificationTemplatesRepository.findByType(likeTemplate);

            NotificationDTO notificationDTO = new NotificationDTO(getPaymentSuccessNotification(notificationTemplate.getTemplate(), tokens));

            createOrUpdateNotification(userCode, List.of(notificationDTO));
        }
        catch (Exception e){
            log.error("error in sendPaymentSuccessNotification {}", e.getMessage(), e);
        }
    }

    private String getPaymentSuccessNotification (String template, Integer tokens) {
        template = template.replaceAll("<<dateTime>>",getFormattedDate());
        return template.replaceAll("<<tokens>>", String.valueOf(tokens));
    }
}
