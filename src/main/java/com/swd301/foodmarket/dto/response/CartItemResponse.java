package com.swd301.foodmarket.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CartItemResponse {
    private Integer productId;
    private Integer mysteryBoxId;
    private String  productName;
    private Integer quantity;
    private BigDecimal price;
    private String  imageUrl;
    private String  shopName;
    private String  itemType;

    private Integer buildComboId;
    private Integer shopOwnerId;
}