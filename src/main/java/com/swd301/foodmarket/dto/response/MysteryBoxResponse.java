package com.swd301.foodmarket.dto.response;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MysteryBoxResponse {

    private Integer id;

    private String boxType;

    private String imageUrl;

    private BigDecimal price;

    private String description;

    private String note;

    private Integer shopOwnerId;
    private List<ProductMysteryResponse> products;


    private Integer totalQuantity;


    private Boolean isActive;

}