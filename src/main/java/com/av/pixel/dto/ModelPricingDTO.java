package com.av.pixel.dto;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class ModelPricingDTO {

    String model;

    Double basePrice;
    Double basePriceMultiplier;

    Double privacyCost;
    Double privacyCostMultiplier;

    Double characterPrice;
    Double characterMultiplier;

    Double seedCost;
    Double seedCostMultiplier;

    Double negativePromptCost;
    Double negativePromptCostMultiplier;


    public Integer getFinalBaseCost() {
        return (int) (basePrice * basePriceMultiplier);
    }

    public Integer getFinalPrivacyCost() {
        return (int) (privacyCost * privacyCostMultiplier);
    }

    public Integer getFinalCharacterCost() {
        return (int) (characterPrice * characterMultiplier);
    }

    public Integer getFinalSeedCost() {
        return (int) (seedCost * seedCostMultiplier);
    }

    public Integer getFinalNegativePromptCost() {
        return (int) (negativePromptCost * negativePromptCostMultiplier);
    }

    public Integer getFinalCost(Integer noOfImages, boolean isPrivate, boolean isSeed, boolean isNegativePrompt, boolean haveCharacterFile) {
        Integer finalCost = noOfImages * getFinalBaseCost();

        if (isPrivate) {
            finalCost += getFinalPrivacyCost();
        }
        if (haveCharacterFile) {
            finalCost += (getFinalCharacterCost() * noOfImages);
        }
        if (isSeed) {
            finalCost += getFinalSeedCost();
        }
        if (isNegativePrompt) {
            finalCost += getFinalNegativePromptCost();
        }
        return finalCost;
    }

}
