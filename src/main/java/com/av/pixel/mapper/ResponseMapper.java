package com.av.pixel.mapper;

import com.av.pixel.response.base.Response;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class ResponseMapper {

    public static <T> ResponseEntity<Response<T>> response (T data, HttpStatus status) {
        return new ResponseEntity<>(new Response<>(status, data), status);
    }

    public static <T> ResponseEntity<Response<T>> response (HttpStatus status, String errorMessage) {
        return new ResponseEntity<>(new Response<>(status, errorMessage), status);
    }

    public static <T> ResponseEntity<Response<T>> response (HttpStatus status, String errorMessage, String displayMessage) {
        return new ResponseEntity<>(new Response<>(status, errorMessage, displayMessage), status);
    }

}
