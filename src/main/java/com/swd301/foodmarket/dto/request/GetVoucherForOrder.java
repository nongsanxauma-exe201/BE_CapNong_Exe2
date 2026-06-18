package com.swd301.foodmarket.dto.request;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class GetVoucherForOrder {
    private Integer shopId;
    private BigDecimal amount;
}
