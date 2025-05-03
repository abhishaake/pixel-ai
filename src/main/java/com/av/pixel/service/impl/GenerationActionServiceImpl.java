package com.av.pixel.service.impl;

import com.av.pixel.dao.Generations;
import com.av.pixel.dao.LikeGenerationMap;
import com.av.pixel.repository.LikeGenerationMapRepository;
import com.av.pixel.service.GenerationActionService;
import com.av.pixel.service.NotificationService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Service
@Slf4j
@AllArgsConstructor
public class GenerationActionServiceImpl implements GenerationActionService {

    private final LikeGenerationMapRepository likeGenerationMapRepository;
    private final MongoTemplate mongoTemplate;
    private final NotificationService notificationService;


    public TreeSet<String> getLikedGenerationsByUserCode (String userCode, List<String> generationIds) {

        List<LikeGenerationMap> likeGenerationMaps = likeGenerationMapRepository.findAllByUserCodeAndGenerationIdInAndDeletedFalse(userCode, generationIds);

        return likeGenerationMaps.stream()
                .map(LikeGenerationMap::getGenerationId)
                .collect(Collectors.toCollection(TreeSet::new));

    }

    @Override
    public String likeGeneration (String userCode, String generationId) {
        Query query = new Query(Criteria.where("_id").is(new ObjectId(generationId)));
        Update update = new Update().inc("likes", 1);

        mongoTemplate.updateFirst(query, update, Generations.class);

        LikeGenerationMap likeGenerationMap = new LikeGenerationMap().setGenerationId(generationId)
                                                .setUserCode(userCode);

        notificationService.sendLikeNotification(generationId, userCode);
        likeGenerationMapRepository.save(likeGenerationMap);
        return "Success";
    }

    @Override
    public String disLikeGeneration (String userCode, String generationId) {
        Query query = new Query(Criteria.where("_id").is(new ObjectId(generationId)));
        Update update = new Update().inc("likes", - 1);

        mongoTemplate.updateFirst(query, update, Generations.class);

        LikeGenerationMap likeGenerationMap = likeGenerationMapRepository.findByUserCodeAndGenerationIdAndDeletedFalse(userCode, generationId);
        likeGenerationMap.setDeleted(true);
        likeGenerationMapRepository.save(likeGenerationMap);
        return "Success";
    }

    @Override
    public String addView (String generationId) {
        Query query = new Query(Criteria.where("_id").is(new ObjectId(generationId)));
        Update update = new Update().inc("views", 1);

        mongoTemplate.updateFirst(query, update, Generations.class);
        return "Success";
    }

}
