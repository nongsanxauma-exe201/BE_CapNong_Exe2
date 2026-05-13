package com.swd301.foodmarket.dto.request;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class PayosPayoutRequest {
    private String clientId;
    private String bankAccountNumber;
    private String bankName;
    private String accountHolder;
    private BigDecimal amount;
    private String orderCode; // mã payout
    private String checksum;
}
