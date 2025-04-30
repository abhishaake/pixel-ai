package com.av.pixel.repository;

import com.av.pixel.dao.GenerationHistory;
import com.av.pixel.repository.base.BaseRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GenerationHistoryRepository extends BaseRepository<GenerationHistory, String> {

}
