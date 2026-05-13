package com.swd301.foodmarket.mapper;

import com.swd301.foodmarket.dto.request.BuildPlanDetailRequest;
import com.swd301.foodmarket.dto.response.BuildPlanResponse;
import com.swd301.foodmarket.entity.BuildPlan;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = {PlanDayMapper.class})
public interface BuildPlanMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "buyer", ignore = true)
    @Mapping(target = "days", ignore = true)
    @Mapping(target = "planName", source = "plan.planName")
    @Mapping(target = "numberOfPeople", source = "plan.numberOfPeople")
    @Mapping(target = "numberOfDays", source = "plan.numberOfDays")
    @Mapping(target = "mealType", source = "plan.mealType")
    @Mapping(target = "targetBudget", source = "plan.targetBudget")
    BuildPlan toEntity(BuildPlanDetailRequest request);

    BuildPlanResponse toResponse(BuildPlan plan);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "buyer", ignore = true)
    @Mapping(target = "days", ignore = true)
    @Mapping(target = "planName", source = "plan.planName")
    @Mapping(target = "numberOfPeople", source = "plan.numberOfPeople")
    @Mapping(target = "numberOfDays", source = "plan.numberOfDays")
    @Mapping(target = "mealType", source = "plan.mealType")
    @Mapping(target = "targetBudget", source = "plan.targetBudget")
    void update(@MappingTarget BuildPlan plan, BuildPlanDetailRequest request);
}