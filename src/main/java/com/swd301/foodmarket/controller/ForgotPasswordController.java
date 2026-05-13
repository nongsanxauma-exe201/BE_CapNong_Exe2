package com.swd301.foodmarket.controller;

import com.swd301.foodmarket.dto.request.ResetPasswordRequest;
import com.swd301.foodmarket.dto.response.ApiResponse;
import com.swd301.foodmarket.service.ForgotPasswordService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/forgot-password")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ForgotPasswordController {

    ForgotPasswordService forgotPasswordService;

    /**
     * Đặt lại mật khẩu
     * Flow: send-otp → verify-otp → reset/{email}
     */
    @PostMapping("/reset/{email}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> resetPassword(
            @PathVariable String email,
            @Valid @RequestBody ResetPasswordRequest request) {

        // Gọi service xử lý toàn bộ logic
        forgotPasswordService.resetPassword(email, request);

        return ResponseEntity.ok(
                ApiResponse.<Map<String, Object>>builder()
                        .code(1000)
                        .message("Đặt lại mật khẩu thành công!")
                        .result(Map.of("success", true))
                        .build()
        );
    }
}