package com.av.pixel.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
@AllArgsConstructor
public class CallbackService {

    private final MonetizationService monetizationService;

    public void handleAdCallback(HttpServletRequest request) {
        log.info("inside handleAdCallback");
        try {
            Map<String, String[]> parameters = captureParameters(request);
            String userCode = String.join(",", parameters.get("user_id"));
            String packageId = String.join(",", parameters.get("custom_data"));
            String transactionId = String.join(",", parameters.get("transaction_id"));
            String timestamp = String.join(",", parameters.get("timestamp"));

            monetizationService.handleAdPayment(userCode, packageId, transactionId, timestamp);

        } catch (Exception e) {
            log.error("handleAdCallback error: {}", e.getMessage(), e);
        }
    }

    private Map<String, String> captureHeaders(HttpServletRequest request) {
        Map<String, String> headers = new HashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();

        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            headers.put(headerName, request.getHeader(headerName));
        }

        return headers;
    }

    private Map<String, String[]> captureParameters(HttpServletRequest request) {
        return request.getParameterMap();
    }

    private String captureRequestBody(HttpServletRequest request) throws Exception {
        StringBuilder body = new StringBuilder();
        BufferedReader reader = request.getReader();
        String line;

        while ((line = reader.readLine()) != null) {
            body.append(line);
        }

        return body.toString();
    }


    private void logRequestDetails(
            Map<String, String> headers,
            Map<String, String[]> parameters,
            String requestBody
    ) {
        StringBuilder stringBuilder = new StringBuilder();

        // Log Headers
        headers.forEach((key, value) ->
                stringBuilder.append("Header - ").append(key).append(": ").append(value).append("\n")
        );
        stringBuilder.append("\n");
        // Log Parameters
        parameters.forEach((key, values) ->
                stringBuilder.append("Param - ").append(key).append(": ").append(String.join(", ", values)).append("\n")
        );

        log.info(stringBuilder.toString() + " \n " + "Request Body: {}", requestBody);
    }
}
