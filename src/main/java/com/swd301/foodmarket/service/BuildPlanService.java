package com.swd301.foodmarket.service;

import com.swd301.foodmarket.dto.request.BuildPlanDetailRequest;
import com.swd301.foodmarket.dto.response.BuildPlanResponse;

import java.util.List;

public interface BuildPlanService {

    // Tạo plan full (có days, meals, items)
    BuildPlanResponse createPlan(BuildPlanDetailRequest request);

    // Lấy chi tiết 1 plan
    BuildPlanResponse getPlanById(Integer id);

    // Lấy tất cả plan của user
    List<BuildPlanResponse> getPlansByUser(Integer userId);

    // Xóa plan
    void deletePlan(Integer id);

    // Update basic info (optional)
    BuildPlanResponse updatePlan(Integer id, BuildPlanDetailRequest request);
}