package com.swd301.foodmarket.service.impl;

import com.swd301.foodmarket.dto.request.ResetPasswordRequest;
import com.swd301.foodmarket.entity.User;
import com.swd301.foodmarket.exception.AppException;
import com.swd301.foodmarket.exception.ErrorCode;
import com.swd301.foodmarket.repository.UserRepository;
import com.swd301.foodmarket.service.ForgotPasswordService;
import com.swd301.foodmarket.service.OtpVerificationService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ForgotPasswordServiceImpl implements ForgotPasswordService {

    UserRepository userRepository;
    PasswordEncoder passwordEncoder;
    OtpVerificationService otpVerificationService;

    @Override
    @Transactional
    public void resetPassword(String email, ResetPasswordRequest request) {
        // 1. Validate: Kiểm tra user tồn tại
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        // 2. Validate: Mật khẩu xác nhận phải khớp
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new AppException(ErrorCode.PASSWORD_MISMATCH);
        }

        // 3. Validate: Mật khẩu mới không được trùng mật khẩu cũ (Optional)
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new AppException(ErrorCode.PASSWORD_SAME_AS_OLD);
        }

        // 4. Cập nhật mật khẩu mới
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // 5. Xóa OTP sau khi reset thành công (bảo mật)
        otpVerificationService.deleteOtpByEmail(email);
    }
}