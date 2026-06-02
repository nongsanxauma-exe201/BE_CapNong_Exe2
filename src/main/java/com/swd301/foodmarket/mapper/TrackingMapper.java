package com.swd301.foodmarket.mapper;

import com.swd301.foodmarket.entity.Tracking;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {ImageMapper.class})
public interface TrackingMapper {
    com.swd301.foodmarket.dto.response.TrackingResponse toTrackingResponse(Tracking tracking);
}
