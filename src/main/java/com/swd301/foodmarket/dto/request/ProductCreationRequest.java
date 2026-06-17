package com.swd301.foodmarket.dto.request;


import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import jakarta.persistence.Column;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

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

    @Min(0) @Max(99)
    Integer discountPercent;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    LocalDateTime discountStartDate;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    LocalDateTime discountEndDate;

}
