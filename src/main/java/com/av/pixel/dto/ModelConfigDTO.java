package com.av.pixel.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.Map;

@Data
@Accessors(chain = true)
public class ModelConfigDTO {

    @JsonProperty("model_type")
    String model;

    Map<String, Object> config;
}
