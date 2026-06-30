package com.swd301.foodmarket.dto.internal;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class ExportOrderRowDto {
    private Integer orderId;
    private LocalDateTime createdAt;
    private Integer buyerId;
    private String buyerFullName;
    private String buyerEmail;
    private String buyerPhone;
    private String shippingAddress;
    private String province;
    private String shopName;
    private String paymentMethod;
    private BigDecimal shippingFee;
    private BigDecimal totalAmount;
    private String status;
    private String itemsSummary;
}
