package com.swd301.foodmarket.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import com.swd301.foodmarket.dto.response.PlanDayResponse;
import com.swd301.foodmarket.entity.PlanDay;

@Mapper(componentModel = "spring", uses = {MealMapper.class})
public interface PlanDayMapper {

    @Mapping(target = "meals", source = "meals")
    PlanDayResponse toResponse(PlanDay day);
}