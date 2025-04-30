package com.av.pixel.dao;

import com.av.pixel.dao.base.BaseEntity;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
@Document("model_config")
public class ModelConfig extends BaseEntity {

    @Field("model_type")
    String model;

    Map<String, Object> config;
}
