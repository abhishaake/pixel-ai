package com.av.pixel.client;

import com.av.pixel.helper.TransformUtil;
import com.av.pixel.request.goenhance.GoEnhanceGenerateRequest;
import com.av.pixel.response.goenhance.GoEnhanceEffectListResponse;
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
import java.util.UUID;

@Component
@Slf4j
public class GoEnhanceClient {

    private static final String BASE_URL = "https://api.goenhance.ai";
    private static final String GENERATE_URL = "/api/v1/videoeffect/generate";
    private static final String JOB_STATUS_URL = "/api/v1/jobs/detail?img_uuid=";
    private static final String EFFECT_LIST_URL = "/api/v1/videoeffect/list";

    private static final String MOCK_VIDEO_URL = "https://cdn.goenhance.ai/video/static/effects/6a03d75e-851a-48f3-837d-4414b7be643d.mp4";

    @Value("${goenhance.api.key}")
    private String apiKey;

    @Value("${goenhance.mock.enabled:false}")
    private boolean mockEnabled;

    private final RestTemplate restTemplate;

    public GoEnhanceClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public GoEnhanceGenerateResponse generateVideoEffect(String effectId, String imageUrl, String resolution) {
        if (mockEnabled) {
            log.info("[GoEnhance][MOCK] generateVideoEffect effectId={}", effectId);
            return mockGenerateResponse();
        }

        String url = BASE_URL + GENERATE_URL;
        GoEnhanceGenerateRequest request = new GoEnhanceGenerateRequest()
                .setArgs(new GoEnhanceGenerateRequest.Args()
                        .setEffectId(effectId)
                        .setResolution(resolution)
                        .setReferenceImg(imageUrl));

        HttpEntity<GoEnhanceGenerateRequest> entity = new HttpEntity<>(request, buildHeaders());
        try {
            ResponseEntity<String> response =
                    restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            GoEnhanceGenerateResponse body = TransformUtil.fromJson(response.getBody(), GoEnhanceGenerateResponse.class);
            if (body == null || !body.isSuccessful()) {
                log.error("[GoEnhance] generate failed effectId={} msg={}", effectId, body != null ? body.getMsg() : "null response");
                return null;
            }
            return body;
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("[GoEnhance] generate failed effectId={} status={} body={}", effectId, e.getStatusCode(), e.getResponseBodyAsString());
            throw e;
        } catch (Exception e) {
            log.error("[GoEnhance] generate failed effectId={} msg={}", effectId, e.getMessage());
            throw e;
        }
    }

    public GoEnhanceJobResponse getJobStatus(String imgUuid) {
        if (mockEnabled) {
            log.info("[GoEnhance][MOCK] getJobStatus uuid={}", imgUuid);
            return mockJobResponse(imgUuid);
        }

        String url = BASE_URL + JOB_STATUS_URL + imgUuid;
        HttpEntity<Void> entity = new HttpEntity<>(buildHeaders());
        try {
            ResponseEntity<String> response =
                    restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            return TransformUtil.fromJson(response.getBody(), GoEnhanceJobResponse.class);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("[GoEnhance] job status failed uuid={} status={} body={}", imgUuid, e.getStatusCode(), e.getResponseBodyAsString());
            throw e;
        } catch (Exception e) {
            log.error("[GoEnhance] job status failed uuid={} msg={}", imgUuid, e.getMessage());
            throw e;
        }
    }

    private GoEnhanceGenerateResponse mockGenerateResponse() {
        GoEnhanceGenerateResponse.Data data = new GoEnhanceGenerateResponse.Data();
        data.setImgUuid(UUID.randomUUID().toString());
        data.setCost(0);

        GoEnhanceGenerateResponse response = new GoEnhanceGenerateResponse();
        response.setCode(0);
        response.setMsg("Success");
        response.setData(data);
        return response;
    }

    private GoEnhanceJobResponse mockJobResponse(String imgUuid) {
        GoEnhanceJobResponse.JsonValue jsonValue = new GoEnhanceJobResponse.JsonValue();
        jsonValue.setType("video");
        jsonValue.setValue(MOCK_VIDEO_URL);
        jsonValue.setDuration(10.0);

        GoEnhanceJobResponse.Data data = new GoEnhanceJobResponse.Data();
        data.setImgUuid(imgUuid);
        data.setStatus("success");
        data.setJson(List.of(jsonValue));

        GoEnhanceJobResponse response = new GoEnhanceJobResponse();
        response.setCode(0);
        response.setMsg("Success");
        response.setData(data);
        return response;
    }

    public GoEnhanceEffectListResponse getEffectList() {
        String url = BASE_URL + EFFECT_LIST_URL;
        HttpEntity<Void> entity = new HttpEntity<>(buildHeaders());
        try {
            ResponseEntity<String> response =
                    restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            return TransformUtil.fromJson(response.getBody(), GoEnhanceEffectListResponse.class);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("[GoEnhance] getEffectList failed status={} body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw e;
        } catch (Exception e) {
            log.error("[GoEnhance] getEffectList failed msg={}", e.getMessage());
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
