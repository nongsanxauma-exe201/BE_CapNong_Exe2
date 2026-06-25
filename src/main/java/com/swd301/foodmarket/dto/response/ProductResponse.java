package com.swd301.foodmarket.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductResponse {
    Integer id;

    String productName;
    String unit;
    BigDecimal stockKg;
    BigDecimal pricePerKg;
    BigDecimal minOrderKg;
    BigDecimal sellingPrice;
    Integer stockQuantity;
    String description;
    String imageUrl;
    Integer shopId;
    String shopName;
    Integer categoryId;
    LocalDate expiryDate;
    String status;
    String categoryName;
    Long sold;

    // Gallery: populated only in getById endpoint, null in list endpoints
    List<ProductImageResponse> images;

    // Discount
    Integer discountPercent;
    LocalDateTime discountStartDate;
    LocalDateTime discountEndDate;
    BigDecimal salePrice;
}