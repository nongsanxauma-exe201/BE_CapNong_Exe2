package com.swd301.foodmarket.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayosPayoutResponse {
    private boolean success;
    private String transactionId;
    private String message;

    private String checkoutUrl;
    private String qrCodeUrl;
}
