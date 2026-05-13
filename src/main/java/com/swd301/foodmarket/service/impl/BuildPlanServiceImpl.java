package com.swd301.foodmarket.service.impl;

import com.swd301.foodmarket.dto.request.BuildPlanDetailRequest;
import com.swd301.foodmarket.dto.response.BuildPlanResponse;
import com.swd301.foodmarket.entity.*;
import com.swd301.foodmarket.enums.MealType;
import com.swd301.foodmarket.exception.AppException;
import com.swd301.foodmarket.exception.ErrorCode;
import com.swd301.foodmarket.mapper.BuildPlanMapper;
import com.swd301.foodmarket.repository.BuildComboRepository;
import com.swd301.foodmarket.repository.BuildPlanRepository;
import com.swd301.foodmarket.repository.UserRepository;
import com.swd301.foodmarket.service.BuildPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class BuildPlanServiceImpl implements BuildPlanService {

    private final BuildPlanRepository planRepository;
    private final BuildComboRepository comboRepository;
    private final UserRepository userRepository;
    private final BuildPlanMapper mapper;

    @Override
    public BuildPlanResponse createPlan(BuildPlanDetailRequest request) {
        User user = getCurrentUser();
        BuildPlan plan = new BuildPlan();
        
        // Map metadata
        mapPlanMetadata(plan, request);
        plan.setBuyer(user);

        // Map days
        plan.setDays(mapDays(request, plan));

        BuildPlan savedPlan = planRepository.saveAndFlush(plan);
        return mapper.toResponse(savedPlan);
    }

    @Override
    public BuildPlanResponse getPlanById(Integer id) {
        BuildPlan plan = planRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.BUILD_PLAN_NOT_FOUND));

        return mapper.toResponse(plan);
    }

    @Override
    public List<BuildPlanResponse> getPlansByUser(Integer userId) {
        return planRepository.findByBuyer_Id(userId).stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Override
    public void deletePlan(Integer id) {
        BuildPlan plan = planRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.BUILD_PLAN_NOT_FOUND));

        if (!plan.getBuyer().getId().equals(getCurrentUser().getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        planRepository.delete(plan);
    }

    @Override
    public BuildPlanResponse updatePlan(Integer id, BuildPlanDetailRequest request) {
        BuildPlan plan = planRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.BUILD_PLAN_NOT_FOUND));
        
        if (!plan.getBuyer().getId().equals(getCurrentUser().getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        // Cập nhật metadata
        mapPlanMetadata(plan, request);

        // Cập nhật days: Xóa danh sách cũ và nạp lại danh sách mới
        if (request.getDays() != null) {
            // Nhờ orphanRemoval = true, việc clear() và addAll() sẽ đồng bộ database
            if (plan.getDays() == null) {
                plan.setDays(new ArrayList<>());
            } else {
                plan.getDays().clear();
            }
            plan.getDays().addAll(mapDays(request, plan));
        }

        return mapper.toResponse(planRepository.saveAndFlush(plan));
    }

    // ================= HELPER METHODS =================

    private void mapPlanMetadata(BuildPlan plan, BuildPlanDetailRequest request) {
        if (request.getPlan() == null) {
            throw new AppException(ErrorCode.INVALID_INPUT); // Hoặc handle theo ý bạn
        }
        plan.setPlanName(request.getPlan().getPlanName());
        plan.setNumberOfPeople(request.getPlan().getNumberOfPeople());
        plan.setNumberOfDays(request.getPlan().getNumberOfDays());
        plan.setMealType(request.getPlan().getMealType());
        plan.setTargetBudget(request.getPlan().getTargetBudget());
    }

    private List<PlanDay> mapDays(BuildPlanDetailRequest request, BuildPlan plan) {
        if (request.getDays() == null) return new ArrayList<>();
        
        return request.getDays().stream().map(dayReq -> {
            PlanDay day = new PlanDay();
            day.setDayIndex(dayReq.getDayIndex());
            day.setPlan(plan);

            List<Meal> meals = dayReq.getMeals().stream().map(mealReq -> {
                Meal meal = new Meal();
                meal.setMealType(MealType.valueOf(mealReq.getMealType()));
                meal.setDay(day);

                List<MealItem> items = mealReq.getItems().stream().map(itemReq -> {
                    BuildCombo combo = comboRepository.findById(itemReq.getComboId())
                            .orElseThrow(() -> new AppException(ErrorCode.BUILD_COMBO_NOT_FOUND));

                    MealItem item = new MealItem();
                    item.setMeal(meal);
                    item.setCombo(combo);
                    item.setQuantity(itemReq.getQuantity());
                    return item;

                }).collect(Collectors.toCollection(ArrayList::new));

                meal.setItems(items);
                return meal;

            }).collect(Collectors.toCollection(ArrayList::new));

            day.setMeals(meals);
            return day;

        }).collect(Collectors.toCollection(ArrayList::new));
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        String email = auth.getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }
}