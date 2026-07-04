package com.swd301.foodmarket.dto.request;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductMysteryRequest {
    private Integer productId;
    private Integer quantity;
    private BigDecimal quantityKg;
}
