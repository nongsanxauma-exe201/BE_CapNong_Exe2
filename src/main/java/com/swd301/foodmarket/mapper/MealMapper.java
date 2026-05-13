package com.swd301.foodmarket.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import com.swd301.foodmarket.dto.response.MealResponse;
import com.swd301.foodmarket.entity.Meal;

@Mapper(componentModel = "spring", uses = {MealItemMapper.class})
public interface MealMapper {

    @Mapping(target = "mealType", source = "mealType")
    @Mapping(target = "items", source = "items")
    MealResponse toResponse(Meal meal);
}