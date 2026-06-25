package com.swd301.foodmarket.dto.request;

import jakarta.persistence.Column;
import lombok.Builder;
import lombok.Data;
import com.swd301.foodmarket.enums.VoucherType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
@Data
public class VoucherCreateRequest {

    private  BigDecimal discountValue;
    private  BigDecimal maxDiscount;
    private  BigDecimal minOrderValue;
    private Integer usageLimit;
    private  LocalDateTime expiryDate;
    private Integer claimLimit;        // tổng số lượt được nhận
    private Integer claimedCount;      // đã có bao nhiêu lượt nhận

    private Integer usedCount;         // đã dùng bao nhiêu lượt

    private Integer perUserLimit;    // mỗi user dùng được mấy lần
    private VoucherType voucherType;
}
