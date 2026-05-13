package com.swd301.foodmarket.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

// dto/request/MysteryBoxCreationRequest.java
@Data
public class MysteryBoxCreationRequest {
    @NotBlank
    private String boxType;

    @NotNull
    private BigDecimal price;

    private String description;
    private String note;

    @NotNull
    private Integer totalQuantity;


    private List<ProductMysteryRequest> products;
}
