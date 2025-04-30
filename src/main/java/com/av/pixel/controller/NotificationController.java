package com.av.pixel.controller;

import com.av.pixel.auth.Authenticated;
import com.av.pixel.dto.UserDTO;
import com.av.pixel.service.NotificationService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.av.pixel.mapper.ResponseMapper.response;

@RestController
@AllArgsConstructor
@Slf4j
@RequestMapping("/api/v1/notification")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/all")
    @Authenticated
    public ResponseEntity<?> getUserNotifications (UserDTO userDTO) {
        return response(notificationService.getUserNotifications(userDTO.getCode()), HttpStatus.OK);
    }

}
