package com.av.pixel.response;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class NotificationResponse {

    List<String> notifications;
}
