package com.swd301.foodmarket.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class MealRequest {
    private String mealType; // BREAKFAST, LUNCH...
    private List<MealItemRequest> items;
}