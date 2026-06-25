package com.swd301.foodmarket.dto.request;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderItemRequest {
    private Integer productId;      // null nếu là mystery box
    private Integer mysteryBoxId;   // null nếu là sản phẩm thường
    private Integer quantity;
    private BigDecimal quantityKg;
    private Integer buildComboId;
}