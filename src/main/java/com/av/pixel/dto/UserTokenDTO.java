package com.av.pixel.dto;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class UserTokenDTO {

    String userCode;
    String authToken;
    String accessToken;
    Long validity;
    boolean expired;
}
