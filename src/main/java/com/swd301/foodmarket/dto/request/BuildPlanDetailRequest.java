package com.swd301.foodmarket.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class BuildPlanDetailRequest {

    private BuildPlanRequest plan;
    private List<PlanDayRequest> days;
}