package com.av.pixel.apple;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AppleReceiptRequest {
    @JsonProperty("receipt-data")
    private String receiptData;
    
    @JsonProperty("password")
    private String password; // App-specific shared secret for auto-renewable subscriptions
    
    @JsonProperty("exclude-old-transactions")
    private Boolean excludeOldTransactions;
} 