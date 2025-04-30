package com.av.pixel.service;

import com.av.pixel.response.NotificationResponse;

public interface NotificationService {

    void addWelcomeNotification (String userCode, Integer tokens);

    void addNewTokenNotification (String userCode, Integer tokens);

    void addLikeNotification (String userCode, String likedBy, String imgUrl);

    void sendLikeNotification (String genId, String likedBy);

    NotificationResponse getUserNotifications (String userCode);

    void sendPaymentSuccessNotification (String userCode, Integer tokens);
}
