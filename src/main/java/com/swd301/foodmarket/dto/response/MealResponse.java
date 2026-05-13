package com.swd301.foodmarket.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class MealResponse {
    private Integer id;
    private String mealType;
    private List<MealItemResponse> items;
}
