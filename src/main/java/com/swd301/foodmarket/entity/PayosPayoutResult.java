package com.swd301.foodmarket.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayosPayoutResult {
    private boolean success;
    private String orderCode;
    private String transactionId;
    private String message;

    // 👇 thêm để FE hiển thị QR
    private String checkoutUrl;
    private String qrCodeUrl;
}