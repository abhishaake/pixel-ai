package com.av.pixel.mapper;

import com.av.pixel.dao.ModelConfig;
import com.av.pixel.dto.ModelConfigDTO;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class ModelConfigMap {

    public static List<ModelConfigDTO> toList(List<ModelConfig> modelConfigs) {
        if (CollectionUtils.isEmpty(modelConfigs)) {
            return Collections.emptyList();
        }
        return modelConfigs.stream()
                .map(ModelConfigMap::toDTO)
                .toList();
    }

    public static ModelConfigDTO toDTO (ModelConfig modelConfig) {
        if (Objects.isNull(modelConfig)) {
            return null;
        }

        return new ModelConfigDTO()
                .setModel(modelConfig.getModel())
                .setConfig(modelConfig.getConfig());
    }

}
