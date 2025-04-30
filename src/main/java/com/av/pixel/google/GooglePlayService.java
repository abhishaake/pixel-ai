package com.av.pixel.google;

import com.av.pixel.google.ProductPurchase;
import java.io.IOException;

public interface GooglePlayService {

    ProductPurchase verifyProductPurchase(String productId, String purchaseToken) throws IOException;

    void acknowledgePurchase(String productId, String purchaseToken, String developerPayload) throws IOException;

    void consumePurchase(String productId, String purchaseToken) throws IOException;
}
