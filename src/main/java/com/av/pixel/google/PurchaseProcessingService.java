package com.av.pixel.google;

import com.av.pixel.enums.PurchaseStatusEnum;
import com.av.pixel.response.PaymentVerificationResponse;

import com.av.pixel.google.ProductPurchase;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@AllArgsConstructor
public class PurchaseProcessingService {

    GooglePlayService apiService;

    public PaymentVerificationResponse processPurchase(String productId, String purchaseToken) {
        try {
            ProductPurchase response = apiService.verifyProductPurchase(productId, purchaseToken);

            if (PurchaseValidator.isSuccessfulPurchase(response)) {

                if (!PurchaseValidator.isConsumed(response)) {
                    apiService.consumePurchase(productId, purchaseToken);
                }

//                if (PurchaseValidator.needsAcknowledgement(response)) {
//                    apiService.acknowledgePurchase(productId, purchaseToken, null);
//                }

                return new PaymentVerificationResponse(
                        PurchaseStatusEnum.SUCCESS,
                        response.getOrderId(),
                        response.getPurchaseTimeMillis()
                );
            }
            else if (PurchaseValidator.isPendingPurchase(response)) {
                return new PaymentVerificationResponse(
                        PurchaseStatusEnum.PENDING,
                        response.getOrderId(),
                        response.getPurchaseTimeMillis()
                );
            }
            else if (PurchaseValidator.isCanceledPurchase(response)) {
                return new PaymentVerificationResponse(
                        PurchaseStatusEnum.FAILED,
                        response.getOrderId(),
                        response.getPurchaseTimeMillis()
                );
            }
            else {
                return new PaymentVerificationResponse(
                        PurchaseStatusEnum.UNKNOWN,
                        response.getOrderId(),
                        response.getPurchaseTimeMillis()
                );
            }
        } catch (Exception e) {
            log.error("[CRITICAL] error : {}", e.getMessage(), e);
            return new PaymentVerificationResponse(
                    PurchaseStatusEnum.ERROR,
                    null,
                    null,
                    e.getMessage(),
                    null
            );
        }
    }

}
