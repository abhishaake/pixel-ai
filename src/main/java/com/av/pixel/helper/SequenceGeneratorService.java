package com.av.pixel.helper;

import com.av.pixel.dao.Counters;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@AllArgsConstructor
public class SequenceGeneratorService {

    private MongoTemplate mongoTemplate;

    public String getNextUserCode() {
        Query query = new Query(Criteria.where("seq_name").is("user_code_sequence"));
        Update update = new Update().inc("seq", 1);

        FindAndModifyOptions options = new FindAndModifyOptions().returnNew(true).upsert(true);
        Counters counter = mongoTemplate.findAndModify(query, update, options, Counters.class);

        return "P" + counter.getSeq();
    }
}
