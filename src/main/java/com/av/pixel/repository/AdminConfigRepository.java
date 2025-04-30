package com.av.pixel.repository;

import com.av.pixel.dao.AdminConfig;
import com.av.pixel.repository.base.BaseRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdminConfigRepository extends BaseRepository<AdminConfig, String> {

    AdminConfig findByKeyAndDeletedFalse(String key);
}
