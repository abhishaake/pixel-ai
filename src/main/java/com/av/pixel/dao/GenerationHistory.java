package com.av.pixel.dao;

import com.av.pixel.dao.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.springframework.data.mongodb.core.mapping.Document;

@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
@Document("generation_history")
public class GenerationHistory extends BaseEntity {

    String generationId;
    String userCode;
    Double cost;
}
