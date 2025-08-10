package com.av.pixel.apple;

import com.apple.itunes.storekit.client.APIException;
import com.apple.itunes.storekit.client.AppStoreServerAPIClient;
import com.apple.itunes.storekit.client.GetTransactionHistoryVersion;
import com.apple.itunes.storekit.migration.ReceiptUtility;
import com.apple.itunes.storekit.model.Environment;
import com.apple.itunes.storekit.model.HistoryResponse;
import com.apple.itunes.storekit.model.TransactionHistoryRequest;
import com.apple.itunes.storekit.model.TransactionInfoResponse;
import com.av.pixel.service.impl.EmailService;

import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class AppStoreService {

    @Value("${apple.app-store.issuer-id}")
    private String issuerId;
    
    @Value("${apple.app-store.key-id}")
    private String keyId;
    
    @Value("${apple.app-store.bundle-id}")
    private String bundleId;

    @Value("${apple.app-store.private-key-path}")
    private Resource privateKey;
    
    @Value("${apple.app-store.shared-secret:null}")
    private String sharedSecret;
    
    @Value("${apple.app-store.sandbox:true}")
    private boolean sandbox;
    
    private final RestTemplate restTemplate;
    private final EmailService emailService;
    private static final String SANDBOX_VERIFY_RECEIPT_URL = "https://sandbox.itunes.apple.com/verifyReceipt";
    private static final String PRODUCTION_VERIFY_RECEIPT_URL = "https://buy.itunes.apple.com/verifyReceipt";
    private static final String APP_STORE_SERVER_API_BASE = "https://api.storekit.itunes.apple.com";
    private static final String VERIFY_TRANSACTION_ENDPOINT = "/inApps/v1/transactions/";

    /**
     * Legacy receipt verification using /verifyReceipt endpoint
     */
    public AppleReceiptResponse verifyReceipt(String receiptData) {
        try {
            String url = PRODUCTION_VERIFY_RECEIPT_URL;
            
            AppleReceiptRequest request = new AppleReceiptRequest();
            request.setReceiptData(receiptData);
//            request.setPassword(sharedSecret);
            request.setPassword(null);
            request.setExcludeOldTransactions(true);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<AppleReceiptRequest> entity = new HttpEntity<>(request, headers);
            
            log.info("Verifying Apple receipt with URL: {}", url);
            ResponseEntity<AppleReceiptResponse> response = restTemplate.postForEntity(url, entity, AppleReceiptResponse.class);
            
            AppleReceiptResponse receiptResponse = response.getBody();
            
            // If status is 21007 (sandbox receipt sent to production), retry with sandbox URL
            if (receiptResponse.getStatus() == 21007) {
                log.info("Sandbox receipt detected, retrying with sandbox URL");
                HttpEntity<AppleReceiptRequest> sandboxEntity = new HttpEntity<>(request, headers);
                ResponseEntity<AppleReceiptResponse> sandboxResponse = restTemplate.postForEntity(SANDBOX_VERIFY_RECEIPT_URL, sandboxEntity, AppleReceiptResponse.class);
                return sandboxResponse.getBody();
            }
            
            return receiptResponse;
            
        } catch (Exception e) {
            log.error("Error verifying Apple receipt: {}", e.getMessage(), e);
            emailService.sendPaymentErrorMail("Apple receipt verification error: " + e.getMessage(), receiptData);
            throw new RuntimeException("Failed to verify Apple receipt", e);
        }
    }

    /**
     * Modern transaction verification using App Store Server API with JWT
     */
    public AppleJWTResponse verifyTransaction(String transactionId) {
        try {
            String jwt = generateJWT();
            String url = APP_STORE_SERVER_API_BASE + VERIFY_TRANSACTION_ENDPOINT + transactionId;
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(jwt);
            
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            
            log.info("Verifying Apple transaction with App Store Server API: {}", transactionId);
            ResponseEntity<AppleJWTResponse> response = restTemplate.exchange(url, HttpMethod.GET, entity, AppleJWTResponse.class);
            
            return response.getBody();
            
        } catch (Exception e) {
            log.error("Error verifying Apple transaction: {}", e.getMessage(), e);
            emailService.sendPaymentErrorMail("Apple transaction verification error: " + e.getMessage(), transactionId);
            throw new RuntimeException("Failed to verify Apple transaction", e);
        }
    }

    /**
     * Get human-readable status message
     */
    public String getStatusMessage(Integer status) {
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

    /**
     * Generate JWT token for App Store Server API authentication
     * Following Apple's specifications: https://developer.apple.com/documentation/appstoreserverapi/generating-json-web-tokens-for-api-requests
     */
    private String generateJWT() {
        try {
            PrivateKey privateKey = loadPrivateKey();
            
            Instant now = Instant.now();
                         Instant expiration = now.plusSeconds(3600); // 1 hour max per Apple docs
             
             return Jwts.builder()
                     .header()
                         .keyId(keyId)
                         .type("JWT")
                         .and()
                    .issuer(issuerId)
                    .issuedAt(Date.from(now))
                    .expiration(Date.from(expiration))
                    .audience()
                        .add("appstoreconnect-v1")
                        .and()
                    .claim("bid", bundleId) // Bundle ID claim recommended by Apple
                    .signWith(privateKey) // ES256 algorithm auto-detected
                    .compact();
                    
        } catch (Exception e) {
            log.error("Error generating JWT: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate JWT for Apple authentication", e);
        }
    }

    /**
     * Load private key from file for JWT signing
     */
    private PrivateKey loadPrivateKey() throws Exception {
        try {
            byte[] keyBytes = privateKey.getInputStream().readAllBytes();
            
            String privateKeyContent = new String(keyBytes)
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");
            
            byte[] decodedKey = Base64.getDecoder().decode(privateKeyContent);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decodedKey);
            KeyFactory keyFactory = KeyFactory.getInstance("EC");
            
            return keyFactory.generatePrivate(keySpec);
            
        } catch (IOException e) {
            log.error("Error reading private key file: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to load Apple private key", e);
        }
    }

    /**
     * Validate Apple receipt response status
     */
    public boolean isValidReceiptStatus(Integer status) {
        return status != null && status == 0; // 0 means valid
    }

    /**
     * Alternative implementation using Apple's official App Store Server API client library
     * This provides better type safety and official Apple support
     */
    public AppleJWTResponse verifyTransactionWithOfficalClient(String transactionId) throws IOException, APIException {
        try {
            // Load private key from resource
            String encodedKey = new String(privateKey.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            
            // Use configured environment
            Environment environment = sandbox ? Environment.SANDBOX : Environment.PRODUCTION;

            // Create official Apple client
            AppStoreServerAPIClient client = new AppStoreServerAPIClient(
                encodedKey, keyId, issuerId, bundleId, environment);

//            ReceiptUtility receiptUtil = new ReceiptUtility();
//            String appReceipt = "";
//            String transactionId2 = receiptUtil.extractTransactionIdFromAppReceipt(appReceipt);

            TransactionInfoResponse response = client.getTransactionInfo(
                    transactionId);
            
            if (response != null && response.getSignedTransactionInfo() != null) {
                log.info("Successfully verified Apple transaction: {}", transactionId);
                
                // Return success response
                AppleJWTResponse jwtResponse = new AppleJWTResponse();
                jwtResponse.setStatus(0); // Success
                jwtResponse.setSignedTransactionInfo(response.getSignedTransactionInfo());
                return jwtResponse;
            } else {
                log.error("No transaction data found for transaction ID: {}", transactionId);
                
                AppleJWTResponse jwtResponse = new AppleJWTResponse();
                jwtResponse.setStatus(1); // Error - no data found
                return jwtResponse;
            }
            
        } catch (APIException e) {
            log.error("Apple API error verifying transaction {}: {}", transactionId, e.getMessage(), e);
//            emailService.sendPaymentErrorMail("Apple API error: " + e.getMessage(), transactionId);
            
            AppleJWTResponse jwtResponse = new AppleJWTResponse();
            jwtResponse.setStatus(2); // Error - API exception
            return jwtResponse;
            
        } catch (Exception e) {
            log.error("Error verifying transaction with official client {}: {}", transactionId, e.getMessage(), e);
//            emailService.sendPaymentErrorMail("Apple official client error: " + e.getMessage(), transactionId);
            throw new RuntimeException("Failed to verify Apple transaction with official client", e);
        }
    }

    /**
     * Extract transaction ID from app receipt using Apple's utility
     */
    public String extractTransactionIdFromReceipt(String appReceipt) {
        try {
            ReceiptUtility receiptUtil = new ReceiptUtility();
            return receiptUtil.extractTransactionIdFromAppReceipt(appReceipt);
        } catch (Exception e) {
            log.error("Error extracting transaction ID from receipt: {}", e.getMessage(), e);
            return null;
        }
    }
}
