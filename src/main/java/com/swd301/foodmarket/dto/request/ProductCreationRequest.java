package com.swd301.foodmarket.dto.request;


import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.swd301.foodmarket.enums.ProductStatus;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductCreationRequest {
    @Column(name = "product_name", nullable = false)
    String productName;

    @Column(length = 50)
    String unit;

    @Column(precision = 18, scale = 2)
    BigDecimal sellingPrice;

    Integer stockQuantity;

    @Column(length = 500)
    String description;

    @Column(length = 500)
    String imageUrl;

    Integer categoryId;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @JsonDeserialize(using = LocalDateDeserializer.class)
    LocalDate expiryDate;

}
