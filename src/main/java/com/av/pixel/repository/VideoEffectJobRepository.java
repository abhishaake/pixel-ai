package com.av.pixel.repository;

import com.av.pixel.dao.VideoEffectJob;
import com.av.pixel.enums.VideoEffectJobStatusEnum;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VideoEffectJobRepository extends MongoRepository<VideoEffectJob, ObjectId> {

    List<VideoEffectJob> findAllByStatusAndDeletedFalse(VideoEffectJobStatusEnum status);
}
