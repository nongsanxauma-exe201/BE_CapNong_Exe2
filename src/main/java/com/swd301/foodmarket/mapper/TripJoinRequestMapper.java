package com.swd301.foodmarket.mapper;

import com.swd301.foodmarket.dto.response.TripJoinRequestResponse;
import com.swd301.foodmarket.entity.TripJoinRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring",uses = {TripMapper.class})
public interface TripJoinRequestMapper {
    @Mapping(target = "farmerId", source = "farmer.id")
    @Mapping(target = "farmerName", source = "farmer.shopName")
    TripJoinRequestResponse entityToResponse(TripJoinRequest tripJoinRequest);

}
