package com.av.pixel.dao;

import com.av.pixel.dao.base.BaseEntity;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.mapping.Document;

@EqualsAndHashCode(callSuper = true)
@Document(collection = "counters")
@Data
public class Counters extends BaseEntity {

    @JsonProperty("seq_name")
    private String seqName;

    private long seq;
}
