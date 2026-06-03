package com.av.pixel.request;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class CodeSignInRequest {

    String code;

    String authToken;
}
