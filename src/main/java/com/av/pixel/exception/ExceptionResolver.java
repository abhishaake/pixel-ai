package com.av.pixel.exception;

import com.av.pixel.response.base.Response;
import io.micrometer.common.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.ConversionNotSupportedException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.lang.Nullable;
import org.springframework.validation.BindException;
import org.springframework.validation.method.MethodValidationException;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import static com.av.pixel.mapper.ResponseMapper.response;

@ControllerAdvice
@Slf4j
public class ExceptionResolver {
    @ExceptionHandler({HttpRequestMethodNotSupportedException.class, HttpMediaTypeNotSupportedException.class, HttpMediaTypeNotAcceptableException.class, MissingPathVariableException.class, MissingServletRequestParameterException.class, MissingServletRequestPartException.class, ServletRequestBindingException.class, MethodArgumentNotValidException.class, HandlerMethodValidationException.class, NoHandlerFoundException.class, NoResourceFoundException.class, AsyncRequestTimeoutException.class, ErrorResponseException.class, MaxUploadSizeExceededException.class, ConversionNotSupportedException.class, TypeMismatchException.class, HttpMessageNotReadableException.class, HttpMessageNotWritableException.class, MethodValidationException.class, BindException.class})
    @Nullable
    public final ResponseEntity<Response<Object>> handleException(Exception ex, WebRequest request) throws Exception {
        switch (ex) {
            case HttpRequestMethodNotSupportedException subEx -> {
                return this.handleException(subEx, subEx.getHeaders(), subEx.getStatusCode(), request, subEx.getMessage());
            }
            case HttpMediaTypeNotSupportedException subEx -> {
                return this.handleException(subEx, subEx.getHeaders(), subEx.getStatusCode(), request, subEx.getMessage());
            }
            case HttpMediaTypeNotAcceptableException subEx -> {
                return this.handleException(subEx, subEx.getHeaders(), subEx.getStatusCode(), request, subEx.getMessage());
            }
            case MissingPathVariableException subEx -> {
                return this.handleException(subEx, subEx.getHeaders(), subEx.getStatusCode(), request, subEx.getMessage());
            }
            case MissingServletRequestParameterException subEx -> {
                return this.handleException(subEx, subEx.getHeaders(), subEx.getStatusCode(), request, subEx.getMessage());
            }
            case MissingServletRequestPartException subEx -> {
                return this.handleException(subEx, subEx.getHeaders(), subEx.getStatusCode(), request, subEx.getMessage());
            }
            case ServletRequestBindingException subEx -> {
                return this.handleException(subEx, subEx.getHeaders(), subEx.getStatusCode(), request, subEx.getMessage());
            }
            case MethodArgumentNotValidException subEx -> {
                return this.handleException(subEx, subEx.getHeaders(), subEx.getStatusCode(), request, subEx.getMessage());
            }
            case HandlerMethodValidationException subEx -> {
                return this.handleException(subEx, subEx.getHeaders(), subEx.getStatusCode(), request, subEx.getMessage());
            }
            case NoHandlerFoundException subEx -> {
                return this.handleException(subEx, subEx.getHeaders(), subEx.getStatusCode(), request, subEx.getMessage());
            }
            case NoResourceFoundException subEx -> {
                return this.handleException(subEx, subEx.getHeaders(), subEx.getStatusCode(), request, subEx.getMessage());
            }
            case AsyncRequestTimeoutException subEx -> {
                return this.handleException(subEx, subEx.getHeaders(), subEx.getStatusCode(), request, subEx.getMessage());
            }
            case ErrorResponseException subEx -> {
                return this.handleException(subEx, subEx.getHeaders(), subEx.getStatusCode(), request, subEx.getMessage());
            }
            case MaxUploadSizeExceededException subEx -> {
                return this.handleException(subEx, subEx.getHeaders(), subEx.getStatusCode(), request, subEx.getMessage());
            }
            case null, default -> {
                HttpHeaders headers = new HttpHeaders();
                return switch (ex) {
                    case ConversionNotSupportedException theEx ->
                            this.handleException(theEx, headers, HttpStatus.INTERNAL_SERVER_ERROR, request, theEx.getMessage());
                    case TypeMismatchException theEx ->
                            this.handleException(theEx, headers, HttpStatus.BAD_REQUEST, request, theEx.getMessage());
                    case HttpMessageNotReadableException theEx ->
                            this.handleException(theEx, headers, HttpStatus.BAD_REQUEST, request, theEx.getMessage());
                    case HttpMessageNotWritableException theEx ->
                            this.handleException(theEx, headers, HttpStatus.INTERNAL_SERVER_ERROR, request, theEx.getMessage());
                    case MethodValidationException subEx ->
                            this.handleException(subEx, headers, HttpStatus.INTERNAL_SERVER_ERROR, request, subEx.getMessage());
                    case BindException theEx -> this.handleException(theEx, headers, HttpStatus.BAD_REQUEST, request, theEx.getMessage());
                    case null, default -> throw ex;
                };
            }
        }
    }

    @ExceptionHandler(Error.class)
    @Nullable
    public ResponseEntity<Response<Object>> handleError (Error e, WebRequest request) throws Exception{
        String token = getToken(request);
        log.error("token : {} , custom error : {}", token, e.getMessage());
        return response(e.getHttpStatus(), e.getHttpStatus().getReasonPhrase(), e.getMessage());
    }

    @ExceptionHandler(AuthenticationException.class)
    @Nullable
    public ResponseEntity<Response<Object>> handleError (AuthenticationException e, WebRequest request) throws Exception{
        String token = getToken(request);
        log.error("token : {} , authentication error : {}", token, e.getMessage());
        return response(e.getHttpStatus(), e.getMessage(), AuthenticationException.getDISPLAY_MSG());
    }

    @ExceptionHandler(Exception.class)
    @Nullable
    public ResponseEntity<Response<Object>> handleError (Exception e, WebRequest request) throws Exception{
        String token = getToken(request);
        log.error("token : {} , general exception error : {}", token, e.getMessage(), e);
        return response(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
    }

    @ExceptionHandler(HttpClientErrorException.class)
    @Nullable
    public ResponseEntity<Response<Object>> handleHttpClientErrorException (HttpClientErrorException e, WebRequest request) throws Exception{
        String token = getToken(request);
        log.error("token : {} , httpClientErrorException exception error : {}", token, e.getMessage());
        return response(HttpStatus.valueOf(e.getStatusCode().value()), e.getMessage());
    }

    private ResponseEntity<Response<Object>> handleException (Throwable t, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request, String errorMessage) {
        String token = getToken(request);
        log.error("token : {} , Error : class -> {} , msg -> {} ", token, t.getClass().getName(), t.getMessage());
        return response(HttpStatus.valueOf(statusCode.value()), errorMessage);
    }

    private String getToken (WebRequest request) {
        String token = request.getHeader("token");
        return StringUtils.isNotEmpty(token) ? token : request.getHeader("user");
    }

}
