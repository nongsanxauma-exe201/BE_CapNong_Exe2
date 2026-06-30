package com.swd301.foodmarket.dto.response;

import com.swd301.foodmarket.enums.VoucherType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VoucherUserResponse {
    private UUID voucherUserId;
    private VoucheResponserUser voucherResponse;
    private Integer quantityUsed  ;
    private Integer quantity  ;
    private LocalDateTime claimAt;

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    public static class VoucheResponserUser {
        private String voucherId;
        private String voucherCode;
        private BigDecimal discountValue;
        private  BigDecimal maxDiscount;
        private  BigDecimal minOrderValue;
        private LocalDateTime expiryDate;
        private VoucherType voucherType;
    }
}
