package com.av.pixel.apple;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AppleJWTRequest {
    @JsonProperty("signedTransactionInfo")
    private String signedTransactionInfo;
} 