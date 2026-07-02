package com.swd301.foodmarket.dto.request;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class GetVoucherForBatch {
    private BigDecimal amount;
    private BigDecimal totalShipping;
}
