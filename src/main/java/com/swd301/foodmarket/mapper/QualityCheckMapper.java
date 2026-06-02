package com.swd301.foodmarket.mapper;

import com.swd301.foodmarket.dto.response.QualityCheckResponse;
import com.swd301.foodmarket.entity.QualityCheck;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {ImageMapper.class})
public interface QualityCheckMapper {
    @Mapping(target = "qualityName", source = "qualityCriteria.qualityName")
    QualityCheckResponse.QualityCheckingResponse toQualityCheckingResponse(QualityCheck qualityCheck);
}
