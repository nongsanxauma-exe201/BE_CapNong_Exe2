package com.swd301.foodmarket.dto.request;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

// dto/request/MysteryBoxUpdateRequest.java
@Data
public class MysteryBoxUpdateRequest {
    private String boxType;
    private BigDecimal price;
    private String description;
    private String note;
    private boolean isActive;
    private Integer totalQuantity;

    private List<ProductMysteryRequest> products;
}
