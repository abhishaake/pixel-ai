package com.av.pixel.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@EqualsAndHashCode(callSuper = true)
@Data
public class AuthenticationException extends Error {

    @Getter
    private static final String DISPLAY_MSG = "Please login";

    public AuthenticationException(){
        super(HttpStatus.UNAUTHORIZED);
    }
}
