package com.swd301.foodmarket.service;

import com.swd301.foodmarket.dto.request.ResetPasswordRequest;

public interface ForgotPasswordService {
    void resetPassword(String email, ResetPasswordRequest request);
}
