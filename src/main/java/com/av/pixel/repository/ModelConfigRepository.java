package com.av.pixel.repository;

import com.av.pixel.dao.ModelConfig;
import com.av.pixel.repository.base.BaseRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ModelConfigRepository extends BaseRepository<ModelConfig, String> {
    List<ModelConfig> findAllByDeletedFalse();
}
