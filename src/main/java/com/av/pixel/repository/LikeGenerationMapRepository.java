package com.av.pixel.repository;

import com.av.pixel.dao.LikeGenerationMap;
import com.av.pixel.repository.base.BaseRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LikeGenerationMapRepository extends BaseRepository<LikeGenerationMap, String> {

    List<LikeGenerationMap> findAllByUserCodeAndGenerationIdInAndDeletedFalse(String userCode, List<String> generationId);

    LikeGenerationMap findByUserCodeAndGenerationIdAndDeletedFalse(String userCode, String generationId);
}
