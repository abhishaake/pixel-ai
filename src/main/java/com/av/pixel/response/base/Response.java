package com.av.pixel.response.base;

import com.av.pixel.helper.DateUtil;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

import java.util.Objects;

@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class Response<T> {

    Long serverTime = DateUtil.currentTimeSec();
    T data;
    boolean success;
    int statusCode;
    String message;
    String displayMessage;

    private static final String ERROR_MSG = "Some error occurred, please try again";

    public Response() {
        this.success = true;
        this.statusCode = HttpStatus.OK.value();
        this.message = HttpStatus.OK.getReasonPhrase();
    }

    public Response(T data) {
        this.data = data;
        this.success = true;
        this.statusCode = HttpStatus.OK.value();
        this.message = HttpStatus.OK.getReasonPhrase();
    }

    public Response(HttpStatus httpStatus, T data) {
        this.data = data;
        this.success = true;
        this.statusCode = httpStatus.value();
        this.message = httpStatus.getReasonPhrase();
    }

    public Response(HttpStatus status, String errorMessage) {
        this.success = false;
        this.statusCode = status.value();
        this.message = errorMessage;
        if (!status.is2xxSuccessful()) {
            this.displayMessage = ERROR_MSG;
        }
    }

    public Response(HttpStatus status, String errorMessage, String displayMessage) {
        this.success = false;
        this.statusCode = status.value();
        this.message = errorMessage;
        this.displayMessage = displayMessage;
    }
}
