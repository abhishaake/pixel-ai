package com.av.pixel.response.goenhance;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class GoEnhanceJobResponse {

    Data data;

    @lombok.Data
    public static class Data {
        String status;
        @JsonProperty("json")
        List<JsonValue> json;
    }

    @lombok.Data
    public static class JsonValue {
        String value;
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
