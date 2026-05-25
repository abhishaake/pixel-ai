package com.av.pixel.response.goenhance;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GoEnhanceJobResponse {

    int code;
    String msg;
    Data data;
    @JsonProperty("request_id")
    String requestId;

    @lombok.Data
    public static class Data {
        @JsonProperty("img_uuid")
        String imgUuid;
        String status;
        String type;
        @JsonProperty("start_time")
        String startTime;
        @JsonProperty("end_time")
        String endTime;
        @JsonProperty("model_id")
        String modelId;
        @JsonProperty("job_type")
        String jobType;
        @JsonProperty("json")
        List<JsonValue> json;
    }

    @lombok.Data
    public static class JsonValue {
        String type;
        String value;
        Double duration;
        @JsonProperty("link_expired_at")
        String linkExpiredAt;
    }

    public boolean isSuccess() {
        return data != null && "success".equals(data.getStatus());
    }

    public boolean isPending() {
        return data != null && "pending".equals(data.getStatus());
    }

    public boolean isProcessing() {
        return data != null && "processing".equals(data.getStatus());
    }

    public String getVideoUrl() {
        if (data == null || data.getJson() == null || data.getJson().isEmpty()) return null;
        return data.getJson().get(0).getValue();
    }
}
