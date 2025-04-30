package com.av.pixel.service.impl;

import com.av.pixel.dao.UserToken;
import com.av.pixel.dto.UserDTO;
import com.av.pixel.dto.UserTokenDTO;
import com.av.pixel.exception.AuthenticationException;
import com.av.pixel.exception.Error;
import com.av.pixel.helper.UserTokenHelper;
import com.av.pixel.mapper.UserMap;
import com.av.pixel.mapper.UserTokenMap;
import com.av.pixel.repository.UserRepository;
import com.av.pixel.repository.UserTokenRepository;
import com.av.pixel.service.UserTokenService;
import io.micrometer.common.util.StringUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@Slf4j
@AllArgsConstructor
public class UserTokenServiceImpl implements UserTokenService {

    private final UserTokenRepository userTokenRepository;
    private final UserRepository userRepository;

    @Override
    public UserTokenDTO registerToken (String userCode, String authToken) {
        UserToken userToken = new UserToken();
        userToken.setAccessToken(UserTokenHelper.generateToken());
        userToken.setAuthToken(authToken);
        userToken.setUserCode(userCode);
        userToken.setValidity(UserTokenHelper.getDefaultValidity());
        userToken.setExpired(false);
        userToken = userTokenRepository.save(userToken);
        return UserTokenMap.toTokenDTO(userToken);
    }

    @Override
    public UserTokenDTO getUserToken (String userCode) {
        UserToken userToken = userTokenRepository.findByUserCodeAndExpiredFalseAndDeletedFalse(userCode);
        return UserTokenMap.toTokenDTO(userToken);
    }

    @Override
    public UserTokenDTO registerToken (String userCode) {
        UserToken userToken = new UserToken();
        userToken.setAccessToken(UserTokenHelper.generateToken());
        userToken.setUserCode(userCode);
        userToken.setValidity(UserTokenHelper.getDefaultValidity());
        userToken.setExpired(false);
        userToken = userTokenRepository.save(userToken);
        return UserTokenMap.toTokenDTO(userToken);
    }

    @Override
    public void expireToken (String accessToken) {
        UserToken userToken = userTokenRepository.findByAccessTokenAndExpiredFalseAndDeletedFalse(accessToken);

        if (Objects.isNull(userToken)) {
            throw new AuthenticationException();
        }

        userToken.setExpired(true);
        userTokenRepository.save(userToken);
    }

    @Override
    public UserDTO getUserFromToken (String accessToken) {
        if (StringUtils.isEmpty(accessToken)) {
            return null;
        }
        // TODO : CACHE

        UserToken userToken = userTokenRepository.findByAccessTokenAndExpiredFalseAndDeletedFalse(accessToken);

        if (Objects.isNull(userToken)){
            return null;
        }

        return UserMap.toUserDTO(userRepository.findByCodeAndDeletedFalse(userToken.getUserCode()));
    }
}
