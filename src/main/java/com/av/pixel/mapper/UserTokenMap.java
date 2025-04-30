package com.av.pixel.mapper;

import com.av.pixel.dao.UserToken;
import com.av.pixel.dto.UserTokenDTO;

import java.util.Objects;

public class UserTokenMap {

    public static UserTokenDTO toTokenDTO(UserToken userToken) {
        if (Objects.isNull(userToken)) {
            return null;
        }
        return new UserTokenDTO()
                .setAuthToken(userToken.getAuthToken())
                .setAccessToken(userToken.getAccessToken())
                .setUserCode(userToken.getUserCode())
                .setValidity(userToken.getValidity())
                .setExpired(userToken.isExpired());
    }

    public static UserToken toEntity (UserTokenDTO userTokenDTO) {
        if (Objects.isNull(userTokenDTO)) {
            return null;
        }
        return new UserToken()
                .setAuthToken(userTokenDTO.getAuthToken())
                .setAccessToken(userTokenDTO.getAccessToken())
                .setUserCode(userTokenDTO.getUserCode())
                .setValidity(userTokenDTO.getValidity())
                .setExpired(userTokenDTO.isExpired());
    }
}
