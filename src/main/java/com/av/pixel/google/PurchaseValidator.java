package com.av.pixel.google;

import com.av.pixel.google.ProductPurchase;

public class PurchaseValidator {
    public static final int PURCHASE_STATE_PURCHASED = 0;
    public static final int PURCHASE_STATE_CANCELED = 1;
    public static final int PURCHASE_STATE_PENDING = 2;

    public static final int ACKNOWLEDGEMENT_STATE_NOT_ACKNOWLEDGED = 0;
    public static final int ACKNOWLEDGEMENT_STATE_ACKNOWLEDGED = 1;

    public static final int CONSUMPTION_STATE_NOT_CONSUMED = 0;
    public static final int CONSUMPTION_STATE_CONSUMED = 1;

    public static boolean isSuccessfulPurchase(ProductPurchase response) {
        return response != null &&
                response.getPurchaseState() != null &&
                response.getPurchaseState() == PURCHASE_STATE_PURCHASED;
    }

    public static boolean needsAcknowledgement(ProductPurchase response) {
        return isSuccessfulPurchase(response) &&
                response.getAcknowledgementState() != null &&
                response.getAcknowledgementState() == ACKNOWLEDGEMENT_STATE_NOT_ACKNOWLEDGED;
    }


    public static boolean isConsumed(ProductPurchase response) {
        return response != null &&
                response.getConsumptionState() != null &&
                response.getConsumptionState() == CONSUMPTION_STATE_CONSUMED;
    }


    public static boolean isPendingPurchase(ProductPurchase response) {
        return response != null &&
                response.getPurchaseState() != null &&
                response.getPurchaseState() == PURCHASE_STATE_PENDING;
    }

    public static boolean isCanceledPurchase(ProductPurchase response) {
        return response != null &&
                response.getPurchaseState() != null &&
                response.getPurchaseState() == PURCHASE_STATE_CANCELED;
    }
}
