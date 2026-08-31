package com.av.pixel.response;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class ImagePrivacyResponse {

    String generationId;
    Boolean privateImage;
    Boolean privacyUnlocked;
    Integer chargedCredits;
    Integer availableCredits;
}
