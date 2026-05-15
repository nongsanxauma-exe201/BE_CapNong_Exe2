package com.swd301.foodmarket.mapper;

import com.swd301.foodmarket.dto.response.QualityCheckResponse;
import com.swd301.foodmarket.entity.QualityCriteria;
import com.swd301.foodmarket.entity.QualityResult;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {QualityCheckMapper.class})
public interface QualityResultMapper {
    @Mapping(target = "qualityCheckingResponses", source = "qualityChecks")
    @Mapping(target = "orderDetailId", source = "orderDetail.id")
    QualityCheckResponse toQualityCheckResponse(QualityResult qualityResult);
}
