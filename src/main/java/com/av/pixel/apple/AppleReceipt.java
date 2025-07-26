package com.av.pixel.apple;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AppleReceipt {
    @JsonProperty("receipt_type")
    private String receiptType;
    
    @JsonProperty("app_item_id")
    private String appItemId;
    
    @JsonProperty("receipt_creation_date")
    private String receiptCreationDate;
    
    @JsonProperty("bundle_id")
    private String bundleId;
    
    @JsonProperty("application_version")
    private String applicationVersion;
    
    @JsonProperty("download_id")
    private String downloadId;
    
    @JsonProperty("version_external_identifier")
    private String versionExternalIdentifier;
    
    @JsonProperty("receipt_creation_date_ms")
    private String receiptCreationDateMs;
    
    @JsonProperty("request_date")
    private String requestDate;
    
    @JsonProperty("request_date_ms")
    private String requestDateMs;
    
    @JsonProperty("original_purchase_date")
    private String originalPurchaseDate;
    
    @JsonProperty("original_purchase_date_ms")
    private String originalPurchaseDateMs;
    
    @JsonProperty("original_application_version")
    private String originalApplicationVersion;
    
    @JsonProperty("in_app")
    private List<AppleInAppPurchase> inApp;
} 