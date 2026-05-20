package com.swd301.foodmarket.dto.request;

import lombok.Data;

import java.time.LocalDateTime;
@Data
public class CreateTripRequest {
    Integer vehicleId;
    Integer warehouseId;
    Double startLat; Double startLng;
    LocalDateTime departureTime;
    Boolean isShareable;
    Double availableCapacityKg;
    Double pricePerKm;
}
