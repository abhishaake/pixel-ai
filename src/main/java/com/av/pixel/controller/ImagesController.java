package com.av.pixel.controller;

import com.av.pixel.auth.Authenticated;
import com.av.pixel.dto.GenerationsDTO;
import com.av.pixel.dto.UserDTO;
import com.av.pixel.enums.PermissionEnum;
import com.av.pixel.request.GenerateRequest;
import com.av.pixel.request.GenerationsFilterRequest;
import com.av.pixel.request.ImageActionRequest;
import com.av.pixel.request.ImagePricingRequest;
import com.av.pixel.response.GenerationsFilterResponse;
import com.av.pixel.response.ImagePricingResponse;
import com.av.pixel.response.base.Response;
import com.av.pixel.service.GenerationsService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.av.pixel.mapper.ResponseMapper.response;

@RestController
@Slf4j
@AllArgsConstructor
@RequestMapping("/api/v1/images")
public class ImagesController {

    GenerationsService imagesService;

    @PostMapping("/filter")
    @Authenticated(permissions = PermissionEnum.ANY)
    public ResponseEntity<Response<GenerationsFilterResponse>> filterImages(UserDTO userDTO, @RequestBody GenerationsFilterRequest imageFilterRequest) {
        return response(imagesService.filterImages(userDTO, imageFilterRequest), HttpStatus.OK);
    }

    @GetMapping("/pricing")
    @Authenticated
    public ResponseEntity<Response<ImagePricingResponse>> getPricing (UserDTO userDTO,
                                                                      @RequestBody ImagePricingRequest imageFilterRequest) {
        return response(imagesService.getPricing(imageFilterRequest), HttpStatus.OK);
    }

    @PostMapping("")
    @Authenticated
    public ResponseEntity<Response<GenerationsDTO>> generate (UserDTO userDTO, @RequestBody GenerateRequest generateRequest) {
        return response(imagesService.generate(userDTO, generateRequest), HttpStatus.CREATED);
    }

    @Authenticated
    @PutMapping("/action")
    public ResponseEntity<Response<String>> performAction (UserDTO userDTO, @RequestBody ImageActionRequest imageActionRequest) {
        return response(imagesService.performAction(userDTO, imageActionRequest), HttpStatus.OK);
    }
}
