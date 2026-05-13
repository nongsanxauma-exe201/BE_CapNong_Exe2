package com.swd301.foodmarket.service;

public interface OtpVerificationService {
    public String sendOtp(String email) ;
    public String verifyOtp(Integer otp, String email);
    void deleteOtpByEmail(String email);
}
