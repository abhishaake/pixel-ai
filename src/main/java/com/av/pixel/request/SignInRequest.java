package com.av.pixel.request;

import lombok.Data;

@Data
public class SignInRequest {
    String firstName;
    String lastName;
    String email;
    String phone;
    String code;
    String password;
    String authToken;
    String imageUrl;
}
