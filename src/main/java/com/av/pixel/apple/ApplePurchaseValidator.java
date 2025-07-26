package com.av.pixel.apple;

import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class ApplePurchaseValidator {

    /**
     * Check if Apple receipt verification was successful
     */
    public static boolean isSuccessfulPurchase(AppleReceiptResponse response) {
        if (response == null) {
            log.warn("Apple receipt response is null");
            return false;
        }
        
        Integer status = response.getStatus();
        if (status == null) {
            log.warn("Apple receipt status is null");
            return false;
        }
        
        // Status 0 means valid receipt
        boolean isValid = status == 0;
        
        if (!isValid) {
            log.warn("Apple receipt verification failed with status: {} ({})", status, getStatusMessage(status));
        }
        
        return isValid;
    }

    /**
     * Check if the purchase is valid and has in-app purchases
     */
    public static boolean hasValidInAppPurchases(AppleReceiptResponse response) {
        if (!isSuccessfulPurchase(response)) {
            return false;
        }
        
        AppleReceipt receipt = response.getReceipt();
        if (receipt == null) {
            log.warn("Apple receipt is null");
            return false;
        }
        
        List<AppleInAppPurchase> inAppPurchases = receipt.getInApp();
        if (inAppPurchases == null || inAppPurchases.isEmpty()) {
            log.warn("No in-app purchases found in receipt");
            return false;
        }
        
        return true;
    }

    /**
     * Find the most recent in-app purchase for a specific product ID
     */
    public static AppleInAppPurchase findLatestPurchase(AppleReceiptResponse response, String productId) {
        if (!hasValidInAppPurchases(response)) {
            return null;
        }
        
        // Check latest_receipt_info first (for subscriptions)
        List<AppleInAppPurchase> latestReceiptInfo = response.getLatestReceiptInfo();
        if (latestReceiptInfo != null && !latestReceiptInfo.isEmpty()) {
            for (AppleInAppPurchase purchase : latestReceiptInfo) {
                if (productId.equals(purchase.getProductId())) {
                    log.info("Found purchase in latest_receipt_info for product: {}", productId);
                    return purchase;
                }
            }
        }
        
        // Fall back to receipt.in_app
        List<AppleInAppPurchase> inAppPurchases = response.getReceipt().getInApp();
        AppleInAppPurchase latestPurchase = null;
        long latestPurchaseTime = 0;
        
        for (AppleInAppPurchase purchase : inAppPurchases) {
            if (productId.equals(purchase.getProductId())) {
                try {
                    long purchaseTime = Long.parseLong(purchase.getPurchaseDateMs());
                    if (purchaseTime > latestPurchaseTime) {
                        latestPurchaseTime = purchaseTime;
                        latestPurchase = purchase;
                    }
                } catch (NumberFormatException e) {
                    log.warn("Invalid purchase date format: {}", purchase.getPurchaseDateMs());
                }
            }
        }
        
        if (latestPurchase != null) {
            log.info("Found purchase in receipt.in_app for product: {}", productId);
        } else {
            log.warn("No purchase found for product: {}", productId);
        }
        
        return latestPurchase;
    }

    /**
     * Check if purchase is pending (Apple doesn't have pending status like Google)
     */
    public static boolean isPendingPurchase(AppleReceiptResponse response) {
        // Apple doesn't have a pending status similar to Google Play
        // Pending renewal info is different and relates to subscription renewals
        return false;
    }

    /**
     * Check if purchase was canceled or refunded
     */
    public static boolean isCanceledPurchase(AppleReceiptResponse response) {
        // Apple receipt verification doesn't directly indicate cancellation
        // You would need to check cancellation_date or use App Store Server API notifications
        // For now, we'll consider any non-successful response as potentially canceled
        return !isSuccessfulPurchase(response);
    }

    /**
     * Validate that the purchase matches expected bundle ID
     */
    public static boolean isValidBundleId(AppleReceiptResponse response, String expectedBundleId) {
        if (!isSuccessfulPurchase(response)) {
            return false;
        }
        
        AppleReceipt receipt = response.getReceipt();
        if (receipt == null || receipt.getBundleId() == null) {
            log.warn("Bundle ID not found in receipt");
            return false;
        }
        
        boolean isValid = expectedBundleId.equals(receipt.getBundleId());
        if (!isValid) {
            log.warn("Bundle ID mismatch. Expected: {}, Actual: {}", expectedBundleId, receipt.getBundleId());
        }
        
        return isValid;
    }

    /**
     * Get human-readable status message
     */
    private static String getStatusMessage(Integer status) {
        if (status == null) return "Unknown status";
        
        return switch (status) {
            case 0 -> "Valid";
            case 21000 -> "App Store could not read the JSON object you provided";
            case 21002 -> "Receipt data property was malformed or missing";
            case 21003 -> "Receipt could not be authenticated";
            case 21004 -> "Shared secret does not match";
            case 21005 -> "Receipt server is not currently available";
            case 21006 -> "Receipt is valid but subscription has expired";
            case 21007 -> "Receipt is from sandbox environment";
            case 21008 -> "Receipt is from production environment";
            case 21009 -> "Internal data access error";
            case 21010 -> "User account cannot be found or has been deleted";
            default -> "Unknown status: " + status;
        };
    }
} 