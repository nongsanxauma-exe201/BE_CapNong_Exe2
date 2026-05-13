package com.swd301.foodmarket.controller;

import com.swd301.foodmarket.dto.request.BuildPlanDetailRequest;
import com.swd301.foodmarket.dto.response.ApiResponse;
import com.swd301.foodmarket.dto.response.BuildPlanResponse;
import com.swd301.foodmarket.service.BuildPlanService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/build_plans")
@RequiredArgsConstructor
public class BuildPlanController {

    private final BuildPlanService buildPlanService;

    // Tạo plan full (days + meals + items)
    @PostMapping
    public ApiResponse<BuildPlanResponse> createPlan(
            @Valid @RequestBody BuildPlanDetailRequest request
    ) {
        return ApiResponse.<BuildPlanResponse>builder()
                .result(buildPlanService.createPlan(request))
                .build();
    }

    //  Lấy chi tiết plan
    @GetMapping("/{id}")
    public ApiResponse<BuildPlanResponse> getPlanById(@PathVariable Integer id) {
        return ApiResponse.<BuildPlanResponse>builder()
                .result(buildPlanService.getPlanById(id))
                .build();
    }

    //  Lấy plan theo user
    @GetMapping("/user/{userId}")
    public ApiResponse<List<BuildPlanResponse>> getPlansByUser(
            @PathVariable Integer userId
    ) {
        return ApiResponse.<List<BuildPlanResponse>>builder()
                .result(buildPlanService.getPlansByUser(userId))
                .build();
    }

    //  Update plan
    @PutMapping("/{id}")
    public ApiResponse<BuildPlanResponse> updatePlan(
            @PathVariable Integer id,
            @Valid @RequestBody BuildPlanDetailRequest request
    ) {
        return ApiResponse.<BuildPlanResponse>builder()
                .result(buildPlanService.updatePlan(id, request))
                .build();
    }

    //  Xóa plan
    @DeleteMapping("/{id}")
    public ApiResponse<String> deletePlan(@PathVariable Integer id) {
        buildPlanService.deletePlan(id);

        return ApiResponse.<String>builder()
                .result("Deleted successfully")
                .build();
    }
}