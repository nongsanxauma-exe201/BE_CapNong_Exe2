package com.swd301.foodmarket.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class MealItemResponse {
    private Integer id;
    private Integer quantity;
    private BuildComboResponse combo;
}