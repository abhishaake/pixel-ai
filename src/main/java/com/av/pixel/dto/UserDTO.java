package com.av.pixel.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class UserDTO {

    String firstName;
    String lastName;
    String email;
    String phone;
    String code;
    String password;
    String accessToken;
    String imageUrl;
    String onboardingDate;

    public String getFullName() {
        return this.firstName + " " + this.lastName;
    }
}
