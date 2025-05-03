package com.av.pixel.client;

import com.av.pixel.exception.IdeogramException;
import com.av.pixel.helper.IdeogramCircuitBreaker;
import com.av.pixel.request.ideogram.BaseRequest;
import com.av.pixel.request.ideogram.ImageRequest;
import com.av.pixel.response.ideogram.BaseResponse;
import com.av.pixel.response.ideogram.ImageResponse;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Random;

@Component
@Slf4j
public class IdeogramClient extends IdeogramBaseClient{

    final RestTemplate restTemplate;
    final IdeogramCircuitBreaker circuitBreaker;

    public IdeogramClient (RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.circuitBreaker = new IdeogramCircuitBreaker(2, 300000);
    }

    private static final String BASE_URL = "https://api.ideogram.ai";
    private static final String GENERATE_IMAGE_URL = "/generate";

    public List<ImageResponse> generateImages(ImageRequest imageRequest) {

        String url = BASE_URL + GENERATE_IMAGE_URL;
        BaseRequest baseRequest = new BaseRequest().setImageRequest(imageRequest);

        return circuitBreaker.execute(
                () -> super.exchange(restTemplate, url, HttpMethod.POST, baseRequest, null, new ParameterizedTypeReference<>() {}),
                this::generateImageFallback);
    }

    public List<ImageResponse> generateImageFallback () {
        throw new IdeogramException();
    }

}
