package com.swd301.foodmarket.dto.response;

import lombok.Data;
import java.util.List;

@Data
public class PayOSPaymentInfoResponse {
    private String code;
    private String desc;
    private PaymentDetails data;

    @Data
    public static class PaymentDetails {
        private String id;
        private long orderCode;
        private long amount;
        private long amountPaid;
        private long amountRemaining;
        private String status;
        private String createdAt;
        private List<Transaction> transactions;
    }

    @Data
    public static class Transaction {
        private String reference;
        private long amount;
        private String accountNumber;
        private String description;
        private String transactionDateTime;
    }
}
