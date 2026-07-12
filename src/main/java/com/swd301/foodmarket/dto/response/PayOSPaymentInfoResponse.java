package com.swd301.foodmarket.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PayOSPaymentInfoResponse {
    private String code;
    private String desc;
    private PaymentDetails data;
    private String signature;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PaymentDetails {
        private String id;
        private long orderCode;
        private long amount;
        private long amountPaid;
        private long amountRemaining;
        private String status;
        private String createdAt;
        // Không map transactions: PayOS trả {} hoặc [] tùy trạng thái → tránh lỗi deserialize
    }
}
