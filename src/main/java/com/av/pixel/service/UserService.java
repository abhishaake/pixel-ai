package com.av.pixel.service;

import com.av.pixel.dao.User;
import com.av.pixel.dto.UserDTO;
import com.av.pixel.request.SignInRequest;
import com.av.pixel.request.SignUpRequest;
import com.av.pixel.response.SignInResponse;
import com.av.pixel.response.SignUpResponse;
import com.av.pixel.response.UserInfoResponse;

import java.util.List;
import java.util.Map;

public interface UserService {

    User createUser(UserDTO userDTO);

    SignUpResponse signUp(SignUpRequest signUpRequest);

    SignInResponse signIn(SignInRequest signInRequest);

    String logout(String accessToken);

    UserInfoResponse getUserInfo (String accessToken);

    Map<String, User> getUserCodeVsUserMap (List<String> userCodes);
}
