package com.swd301.foodmarket.dto.request;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayOSWebhookRequest {

    private String code;   // "00" = success
    private String desc;
    private Data data;
    private String signature; // chữ ký PayOS gửi

    @Getter
    @Setter
    public static class Data {
        private String orderCode;      // payosOrderCode bạn lưu trong Payment
        private Long amount;
        private String status;         // PAID | FAILED | CANCELED
        private String paymentLinkId;
    }
}