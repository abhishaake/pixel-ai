package com.av.pixel.mapper;

import com.av.pixel.dao.ModelPricing;
import com.av.pixel.dto.ModelPricingDTO;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class ModelPricingMap {

    public static ModelPricingDTO toDTO (ModelPricing modelPricing) {
        if (Objects.isNull(modelPricing)) {
            return null;
        }
        return new ModelPricingDTO()
                .setModel(modelPricing.getModel())
                .setBasePrice(modelPricing.getBasePrice())
                .setBasePriceMultiplier(modelPricing.getBasePriceMultiplier())
                .setPrivacyCost(modelPricing.getPrivacyCost())
                .setPrivacyCostMultiplier(modelPricing.getPrivacyCostMultiplier())
                .setSeedCost(modelPricing.getSeedCost())
                .setSeedCostMultiplier(modelPricing.getSeedCostMultiplier())
                .setNegativePromptCost(modelPricing.getNegativePromptCost())
                .setNegativePromptCostMultiplier(modelPricing.getNegativePromptCostMultiplier());
    }

    public static List<ModelPricingDTO> toDtoList (List<ModelPricing> modelPricingList) {
        if (CollectionUtils.isEmpty(modelPricingList)) {
            return Collections.emptyList();
        }
        return modelPricingList.stream()
                .map(ModelPricingMap::toDTO)
                .toList();
    }
}
