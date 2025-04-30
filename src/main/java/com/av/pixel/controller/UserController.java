package com.av.pixel.controller;

import com.av.pixel.auth.Authenticated;
import com.av.pixel.dto.UserDTO;
import com.av.pixel.request.SignInRequest;
import com.av.pixel.request.SignUpRequest;
import com.av.pixel.response.SignInResponse;
import com.av.pixel.response.SignUpResponse;
import com.av.pixel.response.UserInfoResponse;
import com.av.pixel.response.base.Response;
import com.av.pixel.service.UserService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import static com.av.pixel.mapper.ResponseMapper.response;

@RestController
@Slf4j
@RequestMapping("/api/v1/user")
@AllArgsConstructor
public class UserController {

    UserService userService;

    @PostMapping("/signUp")
    public ResponseEntity<Response<SignUpResponse>> signUp (@RequestBody SignUpRequest signUpRequest) {
        return response(userService.signUp(signUpRequest), HttpStatus.CREATED);
    }

    @PostMapping("/signIn")
    public ResponseEntity<Response<SignInResponse>> signIn (@RequestBody SignInRequest signInRequest) {
        return response(userService.signIn(signInRequest), HttpStatus.OK);
    }

    @PostMapping("/logOut")
    @Authenticated
    public ResponseEntity<Response<String>> logOut (UserDTO userDTO) {
        return response(userService.logout(userDTO.getAccessToken()), HttpStatus.OK);
    }

    @GetMapping("/info")
    @Authenticated
    public ResponseEntity<Response<UserInfoResponse>> getUserInfo (UserDTO userDTO) {
        return response(userService.getUserInfo(userDTO.getAccessToken()), HttpStatus.OK);
    }
}
