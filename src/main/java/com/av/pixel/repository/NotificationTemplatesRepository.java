package com.av.pixel.repository;

import com.av.pixel.dao.NotificationTemplate;
import com.av.pixel.repository.base.BaseRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationTemplatesRepository extends BaseRepository<NotificationTemplate, String> {

    NotificationTemplate findByType (String type);

    List<NotificationTemplate> findByTypeIn (List<String> types);
}
