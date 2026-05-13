package com.swd301.foodmarket.dto.request;

import com.swd301.foodmarket.enums.WalletStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class WalletCreationRequest {

    @NotNull
    Integer userId;   // shop owner hoặc admin

    @NotNull
    WalletStatus status;

    BigDecimal totalBalance = BigDecimal.ZERO;
    BigDecimal frozenBalance = BigDecimal.ZERO;
}