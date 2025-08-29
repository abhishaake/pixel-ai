package com.av.pixel.controller;

import com.av.pixel.auth.Authenticated;
import com.av.pixel.dto.UserDTO;
import com.av.pixel.request.BlockUserRequest;
import com.av.pixel.response.BlockedUsersResponse;
import com.av.pixel.response.base.Response;
import com.av.pixel.service.impl.BlockUserService;
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
@RequestMapping("/api/v1/users")
@AllArgsConstructor
public class BlockUserController {

    private final BlockUserService blockUserService;

    @PostMapping("/block")
    @Authenticated
    public ResponseEntity<Response<String>> blockUser (UserDTO userDTO, @RequestBody BlockUserRequest blockUserRequest) {
        return response(blockUserService.blockUser(userDTO, blockUserRequest), HttpStatus.OK);
    }

    @PostMapping("/unblock")
    @Authenticated
    public ResponseEntity<Response<String>> unblockUser (UserDTO userDTO, @RequestBody BlockUserRequest blockUserRequest) {
        return response(blockUserService.unblockUser(userDTO, blockUserRequest), HttpStatus.OK);
    }

    @GetMapping("/blocked")
    @Authenticated
    public ResponseEntity<Response<BlockedUsersResponse>> getAllBlockedUsers (UserDTO userDTO) {
        return response(blockUserService.getAllBlockedUsers(userDTO), HttpStatus.OK);
    }
}
