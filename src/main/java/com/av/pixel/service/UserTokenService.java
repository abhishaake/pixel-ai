package com.av.pixel.service;

import com.av.pixel.dao.User;
import com.av.pixel.dto.UserDTO;
import com.av.pixel.dto.UserTokenDTO;

public interface UserTokenService {

    UserTokenDTO registerToken(String userCode, String authToken);

    UserTokenDTO getUserToken(String userCode);

    UserTokenDTO registerToken(String userCode);

    void expireToken(String accessToken);

    UserDTO getUserFromToken(String accessToken);
}
