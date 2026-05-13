package com.swd301.foodmarket.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductResponse {
    Integer id;

    String productName;
    String unit;
    BigDecimal sellingPrice;
    Integer stockQuantity;
    String description;
    String imageUrl;
    Integer shopId;
    String shopName;
    Integer categoryId;
    LocalDate expiryDate;
    String status;
}