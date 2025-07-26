package com.av.pixel.apple;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApplePendingRenewalInfo {
    @JsonProperty("auto_renew_product_id")
    private String autoRenewProductId;
    
    @JsonProperty("original_transaction_id")
    private String originalTransactionId;
    
    @JsonProperty("product_id")
    private String productId;
    
    @JsonProperty("auto_renew_status")
    private String autoRenewStatus;
} 