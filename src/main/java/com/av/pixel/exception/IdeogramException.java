package com.av.pixel.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class IdeogramException extends RuntimeException {

    private HttpStatus httpStatus;
    private String message;
    private String exceptionMessage;
}
