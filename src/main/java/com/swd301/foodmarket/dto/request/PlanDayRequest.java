package com.swd301.foodmarket.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class PlanDayRequest {
    private Integer dayIndex;
    private List<MealRequest> meals;
}
