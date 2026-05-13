package com.swd301.foodmarket.controller;

import com.swd301.foodmarket.dto.request.BuildPlanRequest;
import com.swd301.foodmarket.dto.response.ApiResponse;
import com.swd301.foodmarket.dto.response.BuildPlanResponse;
import com.swd301.foodmarket.service.PlanGenerateService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/build_plans/generate")
@RequiredArgsConstructor
public class PlanGenerateController {

    private final PlanGenerateService planGenerateService;

    //  Generate plan tự động
    @PostMapping
    public ApiResponse<BuildPlanResponse> generatePlan(
            @Valid @RequestBody BuildPlanRequest request
    ) {
        return ApiResponse.<BuildPlanResponse>builder()
                .result(planGenerateService.generatePlan(request))
                .build();
    }
}