package com.av.pixel.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentVerificationRequest {
    String userCode;
    String productId;
    String purchaseToken; // For Google Play
    String receiptData; // For Apple App Store (base64-encoded receipt)
    String transactionId; // For Apple App Store Server API
    String platform; // "google" or "apple"
    String purchaseId;
}
