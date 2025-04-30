package com.av.pixel.controller;

import com.av.pixel.auth.Authenticated;
import com.av.pixel.dto.UserCreditDTO;
import com.av.pixel.dto.UserDTO;
import com.av.pixel.response.base.Response;
import com.av.pixel.service.UserCreditService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.av.pixel.mapper.ResponseMapper.response;

@RestController
@Slf4j
@RequestMapping("/api/v1/credit")
@AllArgsConstructor
public class CreditController {

    UserCreditService userCreditService;

    @GetMapping
    @Authenticated
    public ResponseEntity<Response<UserCreditDTO>> getUserCredits (UserDTO userDTO) {
        return response(userCreditService.getUserCredit(userDTO.getCode()), HttpStatus.OK);
    }
}
