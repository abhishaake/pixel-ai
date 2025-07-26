package com.av.pixel.controller;

import com.av.pixel.auth.Authenticated;
import com.av.pixel.dto.UserDTO;
import com.av.pixel.helper.TransformUtil;
import com.av.pixel.request.PaymentVerificationRequest;
import com.av.pixel.response.PaymentVerificationResponse;
import com.av.pixel.response.base.Response;
import com.av.pixel.service.MonetizationService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.av.pixel.mapper.ResponseMapper.response;

@RestController
@RequestMapping("/api/v1/payment")
@Slf4j
@AllArgsConstructor
public class PaymentsController {

    MonetizationService monetizationService;

    @PostMapping("/verify")
    @Authenticated
    public ResponseEntity<Response<PaymentVerificationResponse>> handlePaymentVerification (UserDTO userDTO,
                                                                                          @RequestBody PaymentVerificationRequest paymentVerificationRequest) {
        log.info("request : {}", TransformUtil.toJson(paymentVerificationRequest));
        return response(monetizationService.handlePayment(paymentVerificationRequest), HttpStatus.OK);
    }

    @PostMapping("/verify/google")
    @Authenticated
    public ResponseEntity<Response<PaymentVerificationResponse>> handleGooglePaymentVerification (UserDTO userDTO,
                                                                                                 @RequestBody PaymentVerificationRequest paymentVerificationRequest) {
        return response(monetizationService.handleGooglePayment(paymentVerificationRequest), HttpStatus.OK);
    }

    @PostMapping("/verify/apple")
    @Authenticated
    public ResponseEntity<Response<PaymentVerificationResponse>> handleApplePaymentVerification (UserDTO userDTO,
                                                                                                @RequestBody PaymentVerificationRequest paymentVerificationRequest) {
        return response(monetizationService.handleApplePayment(paymentVerificationRequest), HttpStatus.OK);
    }
}
