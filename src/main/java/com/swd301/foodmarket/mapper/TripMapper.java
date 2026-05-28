package com.swd301.foodmarket.mapper;

import com.swd301.foodmarket.dto.request.CreateTripRequest;
import com.swd301.foodmarket.dto.request.UpdateTripRequest;
import com.swd301.foodmarket.dto.response.TripResponse;
import com.swd301.foodmarket.dto.response.TripTrackingResponse;
import com.swd301.foodmarket.entity.Trip;
import com.swd301.foodmarket.entity.TripTracking;
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
    @Mapping(target = "tripTrackingResponse", expression = "java(getTripTrackingResponse(trip))")
    TripResponse entityToResponse(Trip trip);

    default TripTrackingResponse getTripTrackingResponse(Trip trip) {
        if (trip.getTripTrackings() == null || trip.getTripTrackings().isEmpty()) {
            return null;
        }
        TripTracking tracking= trip.getTripTrackings().getLast();
       TripTrackingResponse tripTrackingResponse = TripTrackingResponse.builder()
               .tripId(trip.getId())
               .speed(tracking.getSpeed())
               .heading(tracking.getHeading())
                .lat(tracking.getLat())
               .lng(tracking.getLng())
               .updatedAt(tracking.getCreatedAt().toString())
               .build();
        return tripTrackingResponse;
    }

    Trip requestToEntity(CreateTripRequest request);
    Trip updateEntityFromRequest(UpdateTripRequest request);



}
