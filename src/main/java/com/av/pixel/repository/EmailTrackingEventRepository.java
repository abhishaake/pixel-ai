package com.av.pixel.repository;

import com.av.pixel.dao.EmailTrackingEvent;
import com.av.pixel.enums.EmailTrackingEventType;
import com.av.pixel.repository.base.BaseRepository;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Repository;

@Repository
public interface EmailTrackingEventRepository extends BaseRepository<EmailTrackingEvent, ObjectId> {

    boolean existsByEmailIdAndEventType(String emailId, EmailTrackingEventType eventType);
}
