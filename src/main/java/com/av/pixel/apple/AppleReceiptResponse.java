package com.av.pixel.apple;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AppleReceiptResponse {
    private Integer status;
    
    @JsonProperty("environment")
    private String environment;
    
    @JsonProperty("receipt")
    private AppleReceipt receipt;
    
    @JsonProperty("latest_receipt_info")
    private List<AppleInAppPurchase> latestReceiptInfo;
    
    @JsonProperty("latest_receipt")
    private String latestReceipt;
    
    @JsonProperty("pending_renewal_info")
    private List<ApplePendingRenewalInfo> pendingRenewalInfo;

    private Boolean isSandbox;
} 