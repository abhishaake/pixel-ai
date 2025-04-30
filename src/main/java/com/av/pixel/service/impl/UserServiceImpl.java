package com.av.pixel.service.impl;

import com.av.pixel.dao.User;
import com.av.pixel.dao.UserCredit;
import com.av.pixel.dao.UserToken;
import com.av.pixel.dto.UserCreditDTO;
import com.av.pixel.dto.UserDTO;
import com.av.pixel.dto.UserTokenDTO;
import com.av.pixel.helper.SequenceGeneratorService;
import com.av.pixel.helper.UserHelper;
import com.av.pixel.mapper.UserCreditMap;
import com.av.pixel.mapper.UserMap;
import com.av.pixel.repository.UserRepository;
import com.av.pixel.request.SignInRequest;
import com.av.pixel.request.SignUpRequest;
import com.av.pixel.response.SignInResponse;
import com.av.pixel.response.SignUpResponse;
import com.av.pixel.response.UserInfoResponse;
import com.av.pixel.service.AdminConfigService;
import com.av.pixel.service.NotificationService;
import com.av.pixel.service.UserCreditService;
import com.av.pixel.service.UserService;
import com.av.pixel.service.UserTokenService;
import io.micrometer.common.util.StringUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@AllArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserHelper userHelper;
    private final SequenceGeneratorService sequenceGeneratorService;
    private final UserCreditService userCreditService;
    private final UserTokenService userTokenService;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public User createUser (UserDTO userDTO) {
        userHelper.validateNewUserRequest(userDTO);

        if (StringUtils.isEmpty(userDTO.getPassword())) {
            userDTO.setPassword(userHelper.getEncodedPassword());
        } else {
            userDTO.setPassword(userHelper.encodePassword(userDTO.getPassword()));
        }
        userDTO.setCode(sequenceGeneratorService.getNextUserCode());

        User user = UserMap.toUserEntity(userDTO);

        assert Objects.nonNull(user);

        return userRepository.save(user);
    }

    @Override
    @Transactional
    public SignUpResponse signUp (SignUpRequest signUpRequest) {
        UserDTO userDTO = UserMap.toUserDTO(signUpRequest);
        User user = createUser(userDTO);
        userDTO = UserMap.toUserDTO(user);

        UserCredit userCredit = userCreditService.createNewUserCredit(user);
        UserCreditDTO userCreditDTO = UserCreditMap.userCreditDTO(userCredit);

        notificationService.addWelcomeNotification(user.getCode(), userCredit.getAvailable());

        UserTokenDTO userTokenDTO = null;
        if (StringUtils.isNotEmpty(signUpRequest.getAuthToken())) {
            userTokenDTO = userTokenService.registerToken(user.getCode(), signUpRequest.getAuthToken());
        } else {
            userTokenDTO = userTokenService.registerToken(user.getCode());
        }
        // TODO : cache
        return UserMap.toSignUpResponse(userDTO, userCreditDTO, userTokenDTO);
    }

    @Override
    @Transactional
    public SignInResponse signIn (SignInRequest signInRequest) {
        UserDTO userDTO = UserMap.toUserDTO(signInRequest);

        assert Objects.nonNull(userDTO);
        User user = userRepository.findByEmailAndDeletedFalse(userDTO.getEmail());

        if (Objects.isNull(user)) {
            SignUpResponse signUpResponse = signUp(UserMap.toSignUpRequest(signInRequest));
            return UserMap.toSignInResponse(signUpResponse);
        }
        userDTO = UserMap.toUserDTO(user);

        UserCreditDTO userCreditDTO = userCreditService.getUserCredit(user);

        UserTokenDTO userTokenDTO = userTokenService.getUserToken(user.getCode());
        if (Objects.isNull(userTokenDTO)) {
            userTokenDTO = userTokenService.registerToken(user.getCode(), signInRequest.getAuthToken());
        }

        return UserMap.toResponse(userDTO, userCreditDTO, userTokenDTO);
    }

    @Override
    public String logout (String accessToken) {
        userTokenService.expireToken(accessToken);
        // TODO: clear cache
        return "SUCCESS";
    }

    @Override
    public UserInfoResponse getUserInfo (String accessToken) {
        UserDTO userDTO = userTokenService.getUserFromToken(accessToken);
        UserCreditDTO userCreditDTO = userCreditService.getUserCredit(userDTO.getCode());

        return new UserInfoResponse()
                .setUser(userDTO)
                .setUserCredit(userCreditDTO);
    }

    @Override
    public Map<String, User> getUserCodeVsUserMap (List<String> userCodes) {
        if (CollectionUtils.isEmpty(userCodes)) {
            return new HashMap<>();
        }
        Set<String> userCodeSet = new HashSet<>(userCodes);

        List<User> users = userRepository.findAllByCodeInAndDeletedFalse(userCodeSet.stream().toList());

        return users.stream().collect(Collectors.toMap(User::getCode, u -> u));
    }
}
