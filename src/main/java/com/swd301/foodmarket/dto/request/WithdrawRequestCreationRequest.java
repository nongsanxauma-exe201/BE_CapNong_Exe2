package com.swd301.foodmarket.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class WithdrawRequestCreationRequest {

    @NotNull
    private BigDecimal amount;

    private String reason;

    // 👉 nếu null thì fallback sang bank info của shop owner
    private String bankName;

    private String bankAccountNumber;

    private String bankAccountHolder;
}