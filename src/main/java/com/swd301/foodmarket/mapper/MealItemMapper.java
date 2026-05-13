package com.swd301.foodmarket.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import com.swd301.foodmarket.dto.response.MealItemResponse;
import com.swd301.foodmarket.entity.MealItem;

@Mapper(componentModel = "spring", uses = {BuildComboMapper.class})
public interface MealItemMapper {

    @Mapping(target = "combo", source = "combo")
    MealItemResponse toResponse(MealItem item);
}