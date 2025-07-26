package com.av.pixel.apple;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AppleInAppPurchase {
    @JsonProperty("quantity")
    private String quantity;
    
    @JsonProperty("product_id")
    private String productId;
    
    @JsonProperty("transaction_id")
    private String transactionId;
    
    @JsonProperty("original_transaction_id")
    private String originalTransactionId;
    
    @JsonProperty("purchase_date")
    private String purchaseDate;
    
    @JsonProperty("purchase_date_ms")
    private String purchaseDateMs;
    
    @JsonProperty("purchase_date_pst")
    private String purchaseDatePst;
    
    @JsonProperty("original_purchase_date")
    private String originalPurchaseDate;
    
    @JsonProperty("original_purchase_date_ms")
    private String originalPurchaseDateMs;
    
    @JsonProperty("original_purchase_date_pst")
    private String originalPurchaseDatePst;
    
    @JsonProperty("expires_date")
    private String expiresDate;
    
    @JsonProperty("expires_date_ms")
    private String expiresDateMs;
    
    @JsonProperty("expires_date_pst")
    private String expiresDatePst;
    
    @JsonProperty("web_order_line_item_id")
    private String webOrderLineItemId;
    
    @JsonProperty("is_trial_period")
    private String isTrialPeriod;
    
    @JsonProperty("is_in_intro_offer_period")
    private String isInIntroOfferPeriod;
    
    @JsonProperty("in_app_ownership_type")
    private String inAppOwnershipType;
} 