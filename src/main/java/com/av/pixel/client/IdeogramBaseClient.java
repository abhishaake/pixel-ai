package com.av.pixel.client;

import com.av.pixel.exception.IdeogramException;
import com.av.pixel.exception.IdeogramUnprocessableEntityException;
import com.av.pixel.response.ideogram.BaseResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.UnknownHttpStatusCodeException;

import javax.net.ssl.SSLException;
import java.net.HttpRetryException;
import java.util.List;
import java.util.Objects;

@Slf4j
public class IdeogramBaseClient {

    public <T> List<T> exchange(RestTemplate restTemplate, String url, HttpMethod httpMethod, Object requestBody, HttpHeaders httpHeaders, ParameterizedTypeReference<BaseResponse<T>> type){
        if (Objects.isNull(httpHeaders)) {
            httpHeaders = getDefaultHeaders();
        }
        HttpEntity<Object> entity = new HttpEntity<>(requestBody, httpHeaders);

        ResponseEntity<BaseResponse<T>> response = exchange(restTemplate, url, httpMethod, entity, type);
        return validateAndReturnResponse(response, url, requestBody);
    }

    private <T> List<T> validateAndReturnResponse(ResponseEntity<BaseResponse<T>> response, String url, Object requestBody) {

        if (Objects.isNull(response)){
            printException(url, null, null, "EMPTY RESPONSE", null);
            return null;
        }
        HttpStatusCode statusCode = response.getStatusCode();

        if (HttpStatus.OK.equals(statusCode)) {
            return response.getBody().getData();
        }
        printException(url, statusCode, response.getStatusCode().toString(), null, "status not 200");
        return null;
    }

    public HttpHeaders getDefaultHeaders() {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setAccept(List.of(MediaType.APPLICATION_JSON));
        return httpHeaders;
    }

    private <T> ResponseEntity<BaseResponse<T>> exchange(RestTemplate restTemplate, String url, HttpMethod httpMethod, HttpEntity<Object> entity, ParameterizedTypeReference<BaseResponse<T>> type) {
        try {
            return restTemplate.exchange(url, httpMethod, entity, type);
        } catch (HttpClientErrorException e) {
            String responseBody = e.getResponseBodyAsString();
            printException(url, e.getStatusCode(), e.getStatusText(), responseBody, e.getMessage());

            if (e.getStatusCode() == HttpStatus.UNPROCESSABLE_ENTITY) {
                throw new IdeogramUnprocessableEntityException();
            }

            throw new IdeogramException(HttpStatus.valueOf(e.getStatusCode().value()), null, e.getMessage());
        } catch (HttpServerErrorException e) {
            String responseBody = e.getResponseBodyAsString();
            printException(url, e.getStatusCode(), e.getStatusText(), responseBody, e.getMessage());
            throw new IdeogramException(HttpStatus.valueOf(e.getStatusCode().value()), null, e.getMessage());
        } catch (Exception e) {
            printException(url, null, null, null, e.getMessage());
            throw new IdeogramException(HttpStatus.INTERNAL_SERVER_ERROR, null, e.getMessage());
        }
    }

    private void printException (String url, HttpStatusCode statusCode, String statusText, String responseBody, String exMessage) {
        log.error("[CRITICAL] ideogram exception for url {} : code : {}, text: {}, exMsg: {} , response: {}"
                , url, statusCode, statusText, exMessage, responseBody);
    }

}
