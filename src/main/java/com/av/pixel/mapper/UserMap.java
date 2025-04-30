package com.av.pixel.mapper;

import com.av.pixel.dao.User;
import com.av.pixel.dto.UserCreditDTO;
import com.av.pixel.dto.UserDTO;
import com.av.pixel.dto.UserTokenDTO;
import com.av.pixel.helper.DateUtil;
import com.av.pixel.request.SignInRequest;
import com.av.pixel.request.SignUpRequest;
import com.av.pixel.response.SignInResponse;
import com.av.pixel.response.SignUpResponse;

import java.util.Date;
import java.util.Objects;

public class UserMap {


    public static UserDTO toUserDTO (User user) {
        if (Objects.isNull(user)) {
            return null;
        }

        return new UserDTO()
                .setFirstName(user.getFirstName())
                .setLastName(user.getLastName())
                .setPhone(user.getPhone())
                .setEmail(user.getEmail())
                .setCode(user.getCode())
                .setImageUrl(user.getImageUrl())
                .setOnboardingDate(getOnboardingDate(user.getCreated()));
    }

    private static String getOnboardingDate(Date date) {
        if (Objects.isNull(date)) {
            return "Joined a while ago";
        }
        return "Joined " + DateUtil.formatDate2(date);
    }

    public static User toUserEntity (UserDTO userDTO) {
        if (Objects.isNull(userDTO)) {
            return null;
        }

        return new User()
                .setFirstName(userDTO.getFirstName())
                .setLastName(userDTO.getLastName())
                .setPhone(userDTO.getPhone())
                .setEmail(userDTO.getEmail())
                .setPassword(userDTO.getPassword())
                .setCode(userDTO.getCode())
                .setImageUrl(userDTO.getImageUrl());
    }

    public static UserDTO toUserDTO (SignInRequest signInRequest) {
        if (Objects.isNull(signInRequest)) {
            return null;
        }
        return new UserDTO()
                .setFirstName(signInRequest.getFirstName())
                .setLastName(signInRequest.getLastName())
                .setPhone(signInRequest.getPhone())
                .setEmail(signInRequest.getEmail())
                .setPassword(signInRequest.getPassword())
                .setCode(signInRequest.getCode())
                .setImageUrl(signInRequest.getImageUrl());
    }

    public static UserDTO toUserDTO (SignUpRequest signUpRequest) {
        if (Objects.isNull(signUpRequest)) {
            return null;
        }
        return new UserDTO()
                .setFirstName(signUpRequest.getFirstName())
                .setLastName(signUpRequest.getLastName())
                .setPhone(signUpRequest.getPhone())
                .setEmail(signUpRequest.getEmail())
                .setPassword(signUpRequest.getPassword())
                .setCode(signUpRequest.getCode())
                .setImageUrl(signUpRequest.getImageUrl());
    }

    public static SignInResponse toResponse (UserDTO userDTO, UserCreditDTO userCreditDTO, UserTokenDTO userTokenDTO) {
        return new SignInResponse()
                .setUser(userDTO)
                .setUserToken(userTokenDTO)
                .setUserCredit(userCreditDTO);
    }

    public static SignUpRequest toSignUpRequest (SignInRequest signInRequest) {
        if (Objects.isNull(signInRequest)) {
            return null;
        }
        return new SignUpRequest()
                .setFirstName(signInRequest.getFirstName())
                .setLastName(signInRequest.getLastName())
                .setPhone(signInRequest.getPhone())
                .setEmail(signInRequest.getEmail())
                .setPassword(signInRequest.getPassword())
                .setCode(signInRequest.getCode())
                .setAuthToken(signInRequest.getAuthToken())
                .setImageUrl(signInRequest.getImageUrl());

    }

    public static SignUpResponse toSignUpResponse (UserDTO userDTO, UserCreditDTO userCreditDTO, UserTokenDTO userTokenDTO) {
        return new SignUpResponse()
                .setUser(userDTO)
                .setUserToken(userTokenDTO)
                .setUserCredit(userCreditDTO);
    }

    public static SignInResponse toSignInResponse (SignUpResponse signUpResponse) {
        if (Objects.isNull(signUpResponse)) {
            return null;
        }
        return new SignInResponse()
                .setUser(signUpResponse.getUser())
                .setUserToken(signUpResponse.getUserToken())
                .setUserCredit(signUpResponse.getUserCredit());
    }
}
