package com.av.pixel.repository;

import com.av.pixel.dao.NotificationHistory;
import com.av.pixel.repository.base.BaseRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationHistoryRepository extends BaseRepository<NotificationHistory, String> {

    NotificationHistory findByUserCodeAndDeletedFalse(String userCode);
}
