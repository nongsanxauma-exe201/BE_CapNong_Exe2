package com.swd301.foodmarket.controller;

import com.swd301.foodmarket.dto.request.EmailRequest;
import com.swd301.foodmarket.dto.request.OtpRequest;
import com.swd301.foodmarket.dto.response.ApiResponse;
import com.swd301.foodmarket.service.OtpVerificationService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/otp-verification")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OtpVerificationController {

    OtpVerificationService otpVerificationService;

    @PostMapping("/send-otp")
    public ResponseEntity<ApiResponse<Map<String, Object>>> sendOtp(@RequestBody EmailRequest emailRequest) {
        String message = otpVerificationService.sendOtp(emailRequest.getEmail());
        return ResponseEntity.ok(
                ApiResponse.<Map<String, Object>>builder()
                        .code(1000)
                        .message(message)
                        .result(Map.of("email", emailRequest.getEmail()))
                        .build()
        );
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<Map<String, Object>>> verifyOtp(@RequestBody OtpRequest otpRequest) {
        String message = otpVerificationService.verifyOtp(otpRequest.getOtp(), otpRequest.getEmail());
        return ResponseEntity.ok(
                ApiResponse.<Map<String, Object>>builder()
                        .code(1000)
                        .message(message)
                        .result(Map.of("verified", true))
                        .build()
        );
    }
}
