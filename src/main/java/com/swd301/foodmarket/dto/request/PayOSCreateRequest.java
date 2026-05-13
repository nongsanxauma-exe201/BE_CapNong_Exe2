package com.swd301.foodmarket.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PayOSCreateRequest {
    private Long orderCode;
    private Long amount;
    private String description;
    private String cancelUrl;
    private String returnUrl;
    private String signature;
}
