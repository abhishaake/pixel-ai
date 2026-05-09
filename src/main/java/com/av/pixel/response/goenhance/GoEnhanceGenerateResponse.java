package com.av.pixel.response.goenhance;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class GoEnhanceGenerateResponse {

    int code;
    String msg;
    Data data;
    @JsonProperty("request_id")
    String requestId;

    @lombok.Data
    public static class Data {
        @JsonProperty("img_uuid")
        String imgUuid;
        Integer cost;
    }

    public String getImgUuid() {
        return data != null ? data.getImgUuid() : null;
    }

    public boolean isSuccessful() {
        return code == 0 && data != null;
    }
}
