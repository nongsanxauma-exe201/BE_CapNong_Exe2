package com.swd301.foodmarket.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class BuildPlanResponse {
    private Integer id;
    private String planName;
    private Integer numberOfPeople;
    private Integer numberOfDays;
    private String mealType;
    private BigDecimal targetBudget;
    private List<PlanDayResponse> days;
}