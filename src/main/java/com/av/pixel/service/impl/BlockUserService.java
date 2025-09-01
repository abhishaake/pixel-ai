package com.av.pixel.service.impl;

import com.av.pixel.dao.User;
import com.av.pixel.dao.UserBlockMapping;
import com.av.pixel.dto.UserDTO;
import com.av.pixel.repository.UserBlockMappingRepository;
import com.av.pixel.repository.UserRepository;
import com.av.pixel.request.BlockUserRequest;
import com.av.pixel.response.BlockedUsersResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Slf4j
public class BlockUserService {

    private final UserBlockMappingRepository userBlockMappingRepository;
    private final UserRepository userRepository;

    public String blockUser (UserDTO userDTO, BlockUserRequest blockUserRequest) {
        User user = userRepository.findByCodeAndDeletedFalse(blockUserRequest.getUserCode());
        if (Objects.isNull(user)) {
            return "Failure";
        }
        UserBlockMapping userBlockMapping = new UserBlockMapping()
                .setUserCode(userDTO.getCode())
                .setBlockedUserCode(blockUserRequest.getUserCode())
                .setBlockedUserName(user.getFirstName() + " " + (StringUtils.isNotEmpty(user.getLastName()) ? user.getLastName() : ""));

        userBlockMappingRepository.save(userBlockMapping);
        return "Success";
    }

    public String unblockUser (UserDTO userDTO, BlockUserRequest blockUserRequest) {
        List<UserBlockMapping> userBlockMapping = userBlockMappingRepository
                .findAllByUserCodeAndBlockedUserCodeAndDeletedFalse(userDTO.getCode(), blockUserRequest.getUserCode());

        userBlockMapping.forEach(
                u -> {
                    u.setDeleted(true);
                    userBlockMappingRepository.save(u);
                }
        );

        return "Success";
    }

    public BlockedUsersResponse getAllBlockedUsers(UserDTO userDTO) {
        List<UserBlockMapping> blockedUsers = userBlockMappingRepository.findByUserCodeAndDeletedFalse(userDTO.getCode());

        return new BlockedUsersResponse()
                .setBlockedUsers(
                        blockedUsers.stream()
                                .map(
                                        u -> new BlockedUsersResponse.BlockedUser()
                                                .setUserCode(u.getBlockedUserCode())
                                                .setName(u.getBlockedUserName())
                                )
                                .toList()
                );
    }

    public List<String> getBlockedUsers(String currentUserCode) {
        if (StringUtils.isEmpty(currentUserCode)) {
            return Collections.emptyList();
        }
        List<UserBlockMapping> blockedUsers = userBlockMappingRepository.findByUserCodeAndDeletedFalse(currentUserCode);

        return blockedUsers.stream()
                .map(UserBlockMapping::getBlockedUserCode)
                .toList();
    }
}
