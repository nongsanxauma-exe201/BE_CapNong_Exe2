package com.swd301.foodmarket.mapper;

import com.swd301.foodmarket.dto.response.TripStopResponse;
import com.swd301.foodmarket.entity.TripStop;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring",uses = {UserMapper.class, OrderMapper.class})
public interface TripStropMapper {
    @Mapping(target = "orderResponses", source = "order")
    TripStopResponse toTripStop(TripStop tripStop);
}
