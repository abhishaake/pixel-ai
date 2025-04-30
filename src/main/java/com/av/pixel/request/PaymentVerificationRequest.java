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
    String purchaseToken;
}
