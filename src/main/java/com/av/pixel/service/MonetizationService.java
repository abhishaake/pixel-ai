package com.av.pixel.service;

import com.av.pixel.request.PaymentVerificationRequest;
import com.av.pixel.response.PaymentVerificationResponse;

public interface MonetizationService {

    void handleAdPayment (String userCode, String adIdentifier, String adTxnId, String timestamp);

    PaymentVerificationResponse handleGooglePayment (PaymentVerificationRequest paymentVerificationRequest);
}
