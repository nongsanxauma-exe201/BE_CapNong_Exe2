package com.swd301.foodmarket.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class BuildPlanRequest {
    private String planName;
    private Integer numberOfPeople;
    private Integer numberOfDays;
    private String mealType; // FAMILY / VEGAN...
    private BigDecimal targetBudget;
}