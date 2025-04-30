package com.av.pixel.dao;

import com.av.pixel.dao.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.springframework.data.mongodb.core.mapping.Document;

@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
@Document("model_pricing")
public class ModelPricing extends BaseEntity {

    String model;

    Double basePrice;
    Double basePriceMultiplier;

    Double turboRenderPrice;
    Double turboRenderPriceMultiplier;

    Double qualityRenderPrice;
    Double qualityRenderPriceMultiplier;

    Double privacyCost;
    Double privacyCostMultiplier;

    Double seedCost;
    Double seedCostMultiplier;

    Double negativePromptCost;
    Double negativePromptCostMultiplier;

}
