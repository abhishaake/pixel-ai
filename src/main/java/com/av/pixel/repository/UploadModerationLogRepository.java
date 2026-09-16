package com.av.pixel.repository;

import com.av.pixel.dao.UploadModerationLog;
import com.av.pixel.repository.base.BaseRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UploadModerationLogRepository extends BaseRepository<UploadModerationLog, String> {
}
