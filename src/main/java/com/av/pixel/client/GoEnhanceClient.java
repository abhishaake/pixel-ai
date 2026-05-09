package com.av.pixel.client;

import com.av.pixel.enums.GoEnhanceEffectEnum;
import com.av.pixel.helper.TransformUtil;
import com.av.pixel.request.goenhance.GoEnhanceGenerateRequest;
import com.av.pixel.response.goenhance.GoEnhanceGenerateResponse;
import com.av.pixel.response.goenhance.GoEnhanceJobResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Component
@Slf4j
public class GoEnhanceClient {

    private static final String BASE_URL = "https://api.goenhance.ai";
    private static final String GENERATE_URL = "/api/v2/videoeffect/generate/";
    private static final String JOB_STATUS_URL = "/api/v1/jobs/detail?img_uuid=";

    @Value("${goenhance.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate;

    public GoEnhanceClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public GoEnhanceGenerateResponse generateVideoEffect(GoEnhanceEffectEnum effect, String imageUrl) {
        String url = BASE_URL + GENERATE_URL + effect.getEffectName();
        GoEnhanceGenerateRequest request = new GoEnhanceGenerateRequest()
                .setArgs(new GoEnhanceGenerateRequest.Args().setReferenceImg(imageUrl));

        HttpEntity<GoEnhanceGenerateRequest> entity = new HttpEntity<>(request, buildHeaders());
        try {
            ResponseEntity<GoEnhanceGenerateResponse> response =
                    restTemplate.exchange(url, HttpMethod.POST, entity, GoEnhanceGenerateResponse.class);
            return response.getBody();
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("[GoEnhance] generate failed url={} status={} body={}", url, e.getStatusCode(), e.getResponseBodyAsString());
            throw e;
        } catch (Exception e) {
            log.error("[GoEnhance] generate failed url={} msg={}", url, e.getMessage());
            throw e;
        }
    }

    public GoEnhanceJobResponse getJobStatus(String imgUuid) {
        String url = BASE_URL + JOB_STATUS_URL + imgUuid;
        HttpEntity<Void> entity = new HttpEntity<>(buildHeaders());
        try {
            ResponseEntity<GoEnhanceJobResponse> response =
                    restTemplate.exchange(url, HttpMethod.GET, entity, GoEnhanceJobResponse.class);
            return response.getBody();
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("[GoEnhance] job status failed uuid={} status={} body={}", imgUuid, e.getStatusCode(), e.getResponseBodyAsString());
            throw e;
        } catch (Exception e) {
            log.error("[GoEnhance] job status failed uuid={} msg={}", imgUuid, e.getMessage());
            throw e;
        }
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        return headers;
    }
}
