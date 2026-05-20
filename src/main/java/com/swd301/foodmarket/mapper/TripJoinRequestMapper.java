package com.swd301.foodmarket.mapper;

import com.swd301.foodmarket.dto.response.TripJoinRequestResponse;
import com.swd301.foodmarket.entity.TripJoinRequest;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TripJoinRequestMapper {
    TripJoinRequestResponse entityToResponse(TripJoinRequest tripJoinRequest);
}
