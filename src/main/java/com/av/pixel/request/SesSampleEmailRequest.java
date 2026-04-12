package com.av.pixel.request;

import lombok.Data;

@Data
public class SesSampleEmailRequest {

    private String to;
    private String subject;
    private String htmlBody;
}
