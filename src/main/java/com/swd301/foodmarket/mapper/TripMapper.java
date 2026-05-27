package com.swd301.foodmarket.mapper;

import com.swd301.foodmarket.dto.request.CreateTripRequest;
import com.swd301.foodmarket.dto.request.UpdateTripRequest;
import com.swd301.foodmarket.dto.response.TripResponse;
import com.swd301.foodmarket.entity.Trip;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring",uses = {TripStropMapper.class})
public interface TripMapper {

    @Mapping(target = "ownerId", source = "owner.id")
    @Mapping(target = "ownerName", source = "owner.shopName")
    @Mapping(target = "vehicleId", source = "vehicle.id")
    @Mapping(target = "vehicleNumber", source = "vehicle.vehicleNumber")
    @Mapping(target = "warehouseId", source = "warehouse.id")
    @Mapping(target = "warehouseName", source = "warehouse.name")
    TripResponse entityToResponse(Trip trip);

    Trip requestToEntity(CreateTripRequest request);
    Trip updateEntityFromRequest(UpdateTripRequest request);



}
