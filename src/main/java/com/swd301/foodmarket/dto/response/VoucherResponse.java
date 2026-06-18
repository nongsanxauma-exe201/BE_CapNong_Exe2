package com.swd301.foodmarket.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class VoucherResponse {

    private String voucherId;
    private String voucherCode;
    private BigDecimal discountValue;
    private  BigDecimal maxDiscount;
    private  BigDecimal minOrderValue;
    private LocalDateTime expiryDate;
    private  LocalDateTime createAt;
    private boolean active;
    private Integer claimLimit;
    private Integer claimedCount;

    private Integer usageLimit;
    private Integer usedCount;

    private Integer perUserLimit;
}
