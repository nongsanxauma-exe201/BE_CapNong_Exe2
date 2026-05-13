package com.swd301.foodmarket.dto.request;

import lombok.Data;

@Data
public class OtpRequest {
    private String email;
    private Integer otp;
}