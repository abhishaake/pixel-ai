package com.av.pixel.request;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class SignUpRequest {
    String firstName;
    String lastName;
    String email;
    String phone;
    String code;
    String password;
    String authToken;
    String imageUrl;
}
