package com.swd301.foodmarket.service.impl;

import com.swd301.foodmarket.dto.request.BuildPlanRequest;
import com.swd301.foodmarket.dto.response.BuildPlanResponse;
import com.swd301.foodmarket.entity.*;
import com.swd301.foodmarket.enums.MealType;
import com.swd301.foodmarket.exception.AppException;
import com.swd301.foodmarket.exception.ErrorCode;
import com.swd301.foodmarket.mapper.BuildPlanMapper;
import com.swd301.foodmarket.repository.BuildComboRepository;
import com.swd301.foodmarket.repository.BuildPlanRepository;
import com.swd301.foodmarket.repository.UserRepository;
import com.swd301.foodmarket.service.PlanGenerateService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Transactional
public class PlanGenerateServiceImpl implements PlanGenerateService {

    private final BuildComboRepository comboRepository;
    private final BuildPlanRepository planRepository;
    private final UserRepository userRepository;
    private final BuildPlanMapper mapper;

    @Override
    public BuildPlanResponse generatePlan(BuildPlanRequest request) {

        User user = getCurrentUser();

        BuildPlan plan = BuildPlan.builder()
                .buyer(user)
                .planName(request.getPlanName())
                .numberOfPeople(request.getNumberOfPeople())
                .numberOfDays(request.getNumberOfDays())
                .mealType(request.getMealType())
                .targetBudget(request.getTargetBudget())
                .build();

        List<PlanDay> days = new ArrayList<>();

        for (int i = 1; i <= request.getNumberOfDays(); i++) {

            PlanDay day = new PlanDay();
            day.setDayIndex(i);
            day.setPlan(plan);

            List<Meal> meals = new ArrayList<>();

            for (MealType type : List.of(MealType.BREAKFAST, MealType.LUNCH, MealType.DINNER)) {

                Meal meal = new Meal();
                meal.setMealType(type);
                meal.setDay(day);

                List<BuildCombo> combos = comboRepository.findByMealType(type);

                if (combos.isEmpty()) {
                    throw new AppException(ErrorCode.BUILD_COMBO_NOT_FOUND);
                }

                BuildCombo randomCombo = combos.get(new Random().nextInt(combos.size()));

                MealItem item = new MealItem();
                item.setMeal(meal);
                item.setCombo(randomCombo);

                // scale theo số người (tạm đơn giản)
                item.setQuantity(request.getNumberOfPeople());

                List<MealItem> items = new ArrayList<>();
                items.add(item);
                meal.setItems(items);
                meals.add(meal);
            }

            day.setMeals(meals);
            days.add(day);
        }

        plan.setDays(days);
        BuildPlan savedPlan = planRepository.saveAndFlush(plan);
        return mapper.toResponse(savedPlan);
    }

    // ================= HELPER =================

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