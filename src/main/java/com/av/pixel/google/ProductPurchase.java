package com.av.pixel.google;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.api.client.json.JsonString;
import com.google.api.client.util.Key;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductPurchase {

    @JsonProperty("purchaseTimeMillis")
    private String purchaseTimeMillis;

    @JsonProperty("purchaseState")
    private Integer purchaseState;

    @JsonProperty("consumptionState")
    private Integer consumptionState;

    @JsonProperty("developerPayload")
    private String developerPayload;

    @JsonProperty("orderId")
    private String orderId;

    @JsonProperty("purchaseType")
    private Integer purchaseType;

    @JsonProperty("acknowledgementState")
    private Integer acknowledgementState;

    @JsonProperty("kind")
    private String kind;

    @JsonProperty("purchaseToken")
    private String purchaseToken;

    @JsonProperty("productId")
    private String productId;

    @JsonProperty("quantity")
    private Integer quantity;
}
