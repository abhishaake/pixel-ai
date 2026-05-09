package com.av.pixel.repository;

import com.av.pixel.dao.VideoEffectConfig;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VideoEffectConfigRepository extends MongoRepository<VideoEffectConfig, ObjectId> {

    List<VideoEffectConfig> findAllByDeletedFalse();
}
