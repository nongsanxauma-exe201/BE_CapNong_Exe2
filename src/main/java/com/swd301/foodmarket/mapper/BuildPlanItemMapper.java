package com.swd301.foodmarket.mapper;

import com.swd301.foodmarket.dto.request.BuildPlanItemCreateRequest;
import com.swd301.foodmarket.dto.response.BuildPlanItemResponse;
import com.swd301.foodmarket.entity.BuildPlanItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface BuildPlanItemMapper {

    @Mapping(target = "buildPlan", ignore = true)
    @Mapping(target = "products", ignore = true) // thêm dòng này
    BuildPlanItem toEntity(BuildPlanItemCreateRequest request);

    @Mapping(source = "buildPlan.id", target = "planId")
    BuildPlanItemResponse toResponse(BuildPlanItem item);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "buildPlan", ignore = true)
    @Mapping(target = "products", ignore = true) // thêm dòng này
    void update(@MappingTarget BuildPlanItem item, BuildPlanItemCreateRequest request);
}