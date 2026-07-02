package com.swd301.foodmarket.mapper;

import com.swd301.foodmarket.dto.response.QualityCheckResponse;
import com.swd301.foodmarket.entity.QualityCriteria;
import com.swd301.foodmarket.entity.QualityResult;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = {QualityCheckMapper.class})
public interface QualityResultMapper {
    @Mapping(target = "qualityCheckingResponses", source = "qualityChecks")
    @Mapping(target = "orderDetailId", source = "orderDetail.id")
    @Mapping(target = "orderMysteryBoxId", source = "orderMysteryBox.id")
    @Mapping(target = "damageTier", ignore = true)
    QualityCheckResponse toQualityCheckResponse(QualityResult qualityResult);

    @AfterMapping
    default void mapDamageTier(QualityResult source, @MappingTarget QualityCheckResponse target) {
        if (source.getDamageTier() != null) {
            target.setDamageTier(source.getDamageTier().name());
        }
    }
}
