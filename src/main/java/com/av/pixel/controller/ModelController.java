package com.av.pixel.controller;

import com.av.pixel.response.ModelConfigResponse;
import com.av.pixel.response.base.Response;
import com.av.pixel.service.GenerationsService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.av.pixel.mapper.ResponseMapper.response;

@RestController
@Slf4j
@AllArgsConstructor
@RequestMapping("/api/v1/model")
public class ModelController {

    GenerationsService generationsService;

    @GetMapping("/config")
    public ResponseEntity<Response<ModelConfigResponse>> getModelConfig() {
        return response(generationsService.getModelConfigs(), HttpStatus.OK);
    }
}
