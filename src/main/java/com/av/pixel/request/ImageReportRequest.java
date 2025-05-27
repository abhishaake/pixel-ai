package com.av.pixel.request;

import lombok.Data;

@Data
public class ImageReportRequest {

    private String genId;
    private String imageId;
    private String reason;
}
