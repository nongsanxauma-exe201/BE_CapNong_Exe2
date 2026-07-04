package com.swd301.foodmarket.dto.request;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class VoucherUpdateRequest {

    private BigDecimal discountValue;
    private  BigDecimal maxDiscount;
    private  BigDecimal minOrderValue;
    private Integer usageLimit;
    private LocalDateTime expiryDate;
    private Integer claimLimit;        // tổng số lượt được nhận

    private Integer perUserLimit;    // mỗi user dùng được mấy lần

}
