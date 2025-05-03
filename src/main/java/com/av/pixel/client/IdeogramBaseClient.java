package com.av.pixel.client;

import com.av.pixel.exception.IdeogramException;
import com.av.pixel.exception.IdeogramServerException;
import com.av.pixel.exception.IdeogramUnprocessableEntityException;
import com.av.pixel.response.ideogram.BaseResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import java.util.List;
import java.util.Objects;

@Slf4j
public class IdeogramBaseClient {

    @Value("${ideogram.api.key}")
    private String API_KEY;

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
        httpHeaders.set("Api-Key", API_KEY);
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
            throw new IdeogramServerException(HttpStatus.valueOf(e.getStatusCode().value()), null, e.getMessage());
        } catch (Exception e) {
            printException(url, null, null, null, e.getMessage());
            throw new IdeogramServerException(HttpStatus.INTERNAL_SERVER_ERROR, null, e.getMessage());
        }
    }

    private void printException (String url, HttpStatusCode statusCode, String statusText, String responseBody, String exMessage) {
        log.error("[CRITICAL] ideogram exception for url {} : code : {}, text: {}, exMsg: {} , response: {}"
                , url, statusCode, statusText, exMessage, responseBody);
    }

}
