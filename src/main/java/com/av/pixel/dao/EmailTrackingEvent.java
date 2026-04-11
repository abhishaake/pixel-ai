package com.av.pixel.dao;

import com.av.pixel.dao.base.BaseEntity;
import com.av.pixel.enums.EmailTrackingEventType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
@Document(collection = "email_tracking_events")
public class EmailTrackingEvent extends BaseEntity {

    private EmailTrackingEventType eventType;

    private String userId;

    private String campaignId;

    private String platform;

    private String redirectUrl;

    private String userAgent;

    private String clientIp;

    private Instant eventTimestamp;
}
