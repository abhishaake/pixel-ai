package com.av.pixel.client;

import com.av.pixel.exception.IdeogramException;
import com.av.pixel.helper.IdeogramCircuitBreaker;
import com.av.pixel.request.ideogram.BaseRequest;
import com.av.pixel.request.ideogram.ImageRequest;
import com.av.pixel.response.ideogram.ImageResponse;
import com.av.pixel.service.impl.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Component
@Slf4j
public class IdeogramClient extends IdeogramBaseClient{

    final RestTemplate restTemplate;
    final IdeogramCircuitBreaker circuitBreaker;

    public IdeogramClient (RestTemplate restTemplate, EmailService emailService) {
        super(emailService);
        this.restTemplate = restTemplate;
        this.circuitBreaker = new IdeogramCircuitBreaker(2, 180000);
    }

    private static final String BASE_URL = "https://api.ideogram.ai";
    private static final String GENERATE_IMAGE_URL = "/generate";
    private static final String GENERATE_IMAGE_URL_V2 = "/v1/ideogram-v3/generate";

    public List<ImageResponse> generateImages(ImageRequest imageRequest) {

        String url = BASE_URL + GENERATE_IMAGE_URL;
        BaseRequest baseRequest = new BaseRequest().setImageRequest(imageRequest);

        return circuitBreaker.execute(
                () -> super.exchange(restTemplate, url, HttpMethod.POST, baseRequest, null, new ParameterizedTypeReference<>() {}),
                this::generateImageFallback);
    }

    public List<ImageResponse> generateImagesV2(ImageRequest imageRequest) throws IOException {

        String url = BASE_URL + GENERATE_IMAGE_URL_V2;
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

        // Add fields you want
        body.add("prompt", imageRequest.getPrompt());
        body.add("aspect_ratio", imageRequest.getAspectRatio().getValueV2());
        body.add("magic_prompt_option", imageRequest.getMagicPromptOption().name());
        body.add("seed", imageRequest.getSeed());
        body.add("style_type", imageRequest.getStyleType().name());
        body.add("negative_prompt", imageRequest.getNegativePrompt());
        body.add("num_images", imageRequest.getNumberOfImages());
        body.add("color_palette", imageRequest.getColorPalette());
        body.add("rendering_speed", imageRequest.getModel().isTurboEnabled() ? "TURBO" : "QUALITY");

        MultipartFile multipartFile = imageRequest.getFiles();
        if (multipartFile != null && !multipartFile.isEmpty()) {
            body.add("character_reference_images",
                    new ByteArrayResource(multipartFile.getBytes()) {
                        @Override
                        public String getFilename() {
                            return multipartFile.getOriginalFilename();
                        }
                    });

        }


        return circuitBreaker.execute(
                () -> super.exchangeMultiPart(restTemplate, url, HttpMethod.POST, body, getDefaultHeadersV2(), new ParameterizedTypeReference<>() {}),
                this::generateImageFallback);
    }

    public List<ImageResponse> generateImageFallback () {
        throw new IdeogramException();
    }

}
