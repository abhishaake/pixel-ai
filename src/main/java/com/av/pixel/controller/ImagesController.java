package com.av.pixel.controller;

import com.av.pixel.auth.Authenticated;
import com.av.pixel.dto.GenerationsDTO;
import com.av.pixel.dto.UserDTO;
import com.av.pixel.enums.PermissionEnum;
import com.av.pixel.exception.Error;
import com.av.pixel.helper.TransformUtil;
import com.av.pixel.request.GenerateRequest;
import com.av.pixel.request.GenerationsFilterRequest;
import com.av.pixel.request.ImageActionRequest;
import com.av.pixel.request.ImagePricingRequest;
import com.av.pixel.request.ImageReportRequest;
import com.av.pixel.response.GenerationsFilterResponse;
import com.av.pixel.response.ImagePricingResponse;
import com.av.pixel.response.base.Response;
import com.av.pixel.service.GenerationsService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
        return response(imagesService.generate(userDTO, generateRequest,null), HttpStatus.CREATED);
    }

    @PostMapping(value = "/generate/v2")
    @Authenticated
    public ResponseEntity<Response<GenerationsDTO>> generate(
            UserDTO userDTO,
            @RequestParam(value = "body") String generateRequest,
            @RequestParam(value = "character_reference_images", required = false) MultipartFile file
    ) {
        GenerateRequest generateRequestObject = TransformUtil.fromJson(generateRequest, GenerateRequest.class);
        if(generateRequestObject == null) {
            throw new Error(HttpStatus.BAD_REQUEST, "Invalid request");
        }
        return response(imagesService.generate(userDTO, generateRequestObject, file), HttpStatus.CREATED);
    }

    @Authenticated
    @PutMapping("/action")
    public ResponseEntity<Response<String>> performAction (UserDTO userDTO, @RequestBody ImageActionRequest imageActionRequest) {
        return response(imagesService.performAction(userDTO, imageActionRequest), HttpStatus.OK);
    }

    @Authenticated
    @PutMapping("/view")
    public ResponseEntity<Response<String>> addView (UserDTO userDTO, @RequestBody ImageActionRequest imageActionRequest) {
        return response(imagesService.addView(userDTO, imageActionRequest), HttpStatus.OK);
    }

    @Authenticated(permissions = PermissionEnum.ANY)
    @PostMapping("/report")
    public ResponseEntity<Response<String>> reportImage (UserDTO userDTO, @RequestBody ImageReportRequest imageReportRequest) {
        return response(imagesService.reportImage(userDTO, imageReportRequest), HttpStatus.OK);
    }
}
