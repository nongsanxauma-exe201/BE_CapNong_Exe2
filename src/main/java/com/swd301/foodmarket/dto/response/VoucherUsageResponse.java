package com.swd301.foodmarket.dto.response;

import com.swd301.foodmarket.entity.Order;
import com.swd301.foodmarket.entity.User;
import com.swd301.foodmarket.entity.Voucher;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class VoucherUsageResponse {
    private UUID id;
    private BigDecimal discountAmount;
    private LocalDateTime usedAt ;
    private String voucherId;
    private String voucherCode;
    private BigDecimal discountValue;
    private  BigDecimal maxDiscount;
}
