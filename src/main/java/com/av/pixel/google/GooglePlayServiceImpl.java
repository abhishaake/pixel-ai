package com.av.pixel.google;

import com.google.auth.oauth2.GoogleCredentials;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.av.pixel.google.ProductPurchase;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class GooglePlayServiceImpl implements GooglePlayService {

    private static final String ANDROID_PUBLISHER_SCOPE = "https://www.googleapis.com/auth/androidpublisher";
    private static final String BASE_URL = "https://androidpublisher.googleapis.com/androidpublisher/v3/applications";

    @Value("${google.service-account-key}")
    private Resource serviceAccountKey;

    @Value("${app.package-name}")
    private String packageName;

    private final RestTemplate restTemplate;
    private GoogleCredentials credentials;


    @PostConstruct
    public void initialize() throws IOException {
        this.credentials = GoogleCredentials
                .fromStream(serviceAccountKey.getInputStream())
                .createScoped(Collections.singleton(ANDROID_PUBLISHER_SCOPE));
    }

    private HttpHeaders createAuthHeaders() throws IOException {
        credentials.refreshIfExpired();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(credentials.getAccessToken().getTokenValue());
        return headers;
    }

    @Override
    public ProductPurchase verifyProductPurchase(String productId, String purchaseToken) throws IOException {
        String url = String.format("%s/%s/purchases/products/%s/tokens/%s",
                BASE_URL, packageName, productId, purchaseToken);

        HttpEntity<Void> requestEntity = new HttpEntity<>(createAuthHeaders());
        ResponseEntity<ProductPurchase> response = restTemplate.exchange(
                url, HttpMethod.GET, requestEntity, ProductPurchase.class);

        return response.getBody();
    }

    @Override
    public void acknowledgePurchase(String productId, String purchaseToken, String developerPayload) throws IOException {
        String url = String.format("%s/%s/purchases/products/%s/tokens/%s:acknowledge",
                BASE_URL, packageName, productId, purchaseToken);

        Map<String, String> requestBody = new HashMap<>();
        if (developerPayload != null && !developerPayload.isEmpty()) {
            requestBody.put("developerPayload", developerPayload);
        }

        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(requestBody, createAuthHeaders());
        restTemplate.exchange(url, HttpMethod.POST, requestEntity, Void.class);
    }

    @Override
    public void consumePurchase(String productId, String purchaseToken) throws IOException {
        String url = String.format("%s/%s/purchases/products/%s/tokens/%s:consume",
                BASE_URL, packageName, productId, purchaseToken);

        HttpEntity<Void> requestEntity = new HttpEntity<>(createAuthHeaders());
        restTemplate.exchange(url, HttpMethod.POST, requestEntity, Void.class);
    }
}
