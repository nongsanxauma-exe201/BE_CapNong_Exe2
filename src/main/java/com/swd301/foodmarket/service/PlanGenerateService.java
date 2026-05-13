package com.swd301.foodmarket.service;

import com.swd301.foodmarket.dto.request.BuildPlanRequest;
import com.swd301.foodmarket.dto.response.BuildPlanResponse;

public interface PlanGenerateService {

    // Generate plan tự động theo input
    BuildPlanResponse generatePlan(BuildPlanRequest request);
}