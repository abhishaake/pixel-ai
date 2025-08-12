package com.av.pixel.google;

import com.av.pixel.apple.AppStoreService;
import com.av.pixel.apple.AppleInAppPurchase;
import com.av.pixel.apple.AppleJWTResponse;
import com.av.pixel.apple.ApplePurchaseValidator;
import com.av.pixel.apple.AppleReceiptResponse;
import com.av.pixel.enums.PurchaseStatusEnum;
import com.av.pixel.helper.TransformUtil;
import com.av.pixel.response.PaymentVerificationResponse;

import com.av.pixel.google.ProductPurchase;
import com.av.pixel.service.impl.EmailService;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Service
@Slf4j
@RequiredArgsConstructor
public class PurchaseProcessingService {

    @Value("${apple.app-store.bundle-id}")
    private String appleBundleId;

    final GooglePlayService apiService;
    final AppStoreService appStoreService;
    final EmailService emailService;

    public PaymentVerificationResponse processGooglePurchase(String productId, String purchaseToken) {
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
            emailService.sendPaymentErrorMail( "processPurchase error : " + e.getMessage(), TransformUtil.toJson( Arrays.stream(e.getStackTrace()).limit(10)));
            return new PaymentVerificationResponse(
                    PurchaseStatusEnum.ERROR,
                    null,
                    null,
                    e.getMessage(),
                    null
            );
        }
    }

    public PaymentVerificationResponse processApplePurchase(String productId, String receiptData) {
        try {
            AppleReceiptResponse response = appStoreService.verifyReceipt(receiptData);

            if (ApplePurchaseValidator.isSuccessfulPurchase(response)) {
                
                // Validate bundle ID
                if (!ApplePurchaseValidator.isValidBundleId(response, appleBundleId)) {
                    return new PaymentVerificationResponse(
                            PurchaseStatusEnum.ERROR,
                            null,
                            null,
                            "Bundle ID mismatch",
                            null
                    );
                }

                // Find the specific purchase for this product
                AppleInAppPurchase purchase = ApplePurchaseValidator.findLatestPurchase(response, productId);
                if (purchase == null) {
                    return new PaymentVerificationResponse(
                            PurchaseStatusEnum.ERROR,
                            null,
                            null,
                            "Product not found in receipt",
                            null
                    );
                }

                return new PaymentVerificationResponse(
                        PurchaseStatusEnum.SUCCESS,
                        purchase.getTransactionId(),
                        purchase.getPurchaseDateMs(),
                        null,
                        null,
                        response.getIsSandbox()
                );
            }
            else if (ApplePurchaseValidator.isCanceledPurchase(response)) {
                return new PaymentVerificationResponse(
                        PurchaseStatusEnum.FAILED,
                        null,
                        null,
                        appStoreService.getStatusMessage(response.getStatus()),
                        null
                );
            }
            else {
                return new PaymentVerificationResponse(
                        PurchaseStatusEnum.ERROR,
                        null,
                        null,
                        appStoreService.getStatusMessage(response.getStatus()),
                        null
                );
            }
        } catch (Exception e) {
            log.error("[CRITICAL] Apple purchase processing error : {}", e.getMessage(), e);
            emailService.sendPaymentErrorMail("Apple purchase processing error : " + e.getMessage(), TransformUtil.toJson(Arrays.stream(e.getStackTrace()).limit(10)));
            return new PaymentVerificationResponse(
                    PurchaseStatusEnum.ERROR,
                    null,
                    null,
                    e.getMessage(),
                    null
            );
        }
    }

    public PaymentVerificationResponse processAppleTransaction(String transactionId) {
        try {
            AppleJWTResponse response = appStoreService.verifyTransactionWithOfficalClient(transactionId);

            if (response != null && response.getStatus() != null && response.getStatus() == 0) {
                // For JWT response, we get the transaction directly
                // The transactionId parameter is the Apple transaction ID we're verifying
                return new PaymentVerificationResponse(
                        PurchaseStatusEnum.SUCCESS,
                        transactionId,
                        String.valueOf(System.currentTimeMillis()) // Current time as fallback
                );
            }
            else {
                String errorMsg = response != null && response.getStatus() != null 
                    ? "Transaction verification failed with status: " + response.getStatus()
                    : "Transaction verification failed";
                    
                return new PaymentVerificationResponse(
                        PurchaseStatusEnum.ERROR,
                        null,
                        null,
                        errorMsg,
                        null
                );
            }
        } catch (Exception e) {
            log.error("[CRITICAL] Apple transaction verification error : {}", e.getMessage(), e);
            emailService.sendPaymentErrorMail("Apple transaction verification error : " + e.getMessage(), transactionId);
            return new PaymentVerificationResponse(
                    PurchaseStatusEnum.ERROR,
                    null,
                    null,
                    e.getMessage(),
                    null
            );
        }
    }

    // Legacy method for backward compatibility
    public PaymentVerificationResponse processPurchase(String productId, String purchaseToken) {
        return processGooglePurchase(productId, purchaseToken);
    }

}
