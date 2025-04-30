package com.av.pixel.dao;

import com.av.pixel.dao.base.BaseEntity;
import com.av.pixel.dto.NotificationDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Document(collection = "notification_history")
@Data
@Accessors(chain = true)
public class NotificationHistory extends BaseEntity {

    String userCode;
    List<NotificationDTO> notifications;
}
