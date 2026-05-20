package com.swd301.foodmarket.mapper;

import com.swd301.foodmarket.dto.response.VehicleResponse;
import com.swd301.foodmarket.entity.Vehicle;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring",uses = {VehicleMapper.class})
public interface VehicleMapper {
    VehicleResponse toVehicleResponse(Vehicle vehicle);
    Vehicle toVehicle(VehicleResponse vehicleResponse);
}
