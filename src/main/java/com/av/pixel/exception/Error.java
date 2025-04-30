package com.av.pixel.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.http.HttpStatus;

@EqualsAndHashCode(callSuper = true)
@Data
public class Error extends RuntimeException {

    private HttpStatus httpStatus;
    private Object data;

    public Error(){
        this.httpStatus = HttpStatus.BAD_REQUEST;
    }

    public Error(final HttpStatus httpStatus, final String description) {
        super(description);
        this.httpStatus = httpStatus;
    }

    public Error(final HttpStatus httpStatus, final String description, final Object data) {
        super(description);
        this.httpStatus = httpStatus;
        this.data = data;
    }

    public Error(final HttpStatus httpStatus) {
        super(httpStatus.getReasonPhrase());
        this.httpStatus = httpStatus;
    }

    public Error(final String message) {
        super(message);
        this.httpStatus = HttpStatus.BAD_REQUEST;
    }

}
