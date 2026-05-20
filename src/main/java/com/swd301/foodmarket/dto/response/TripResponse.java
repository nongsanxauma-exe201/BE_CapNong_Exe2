package com.swd301.foodmarket.dto.response;

import com.swd301.foodmarket.enums.TripStatus;
import lombok.Data;

import java.time.LocalDateTime;
@Data
public class TripResponse {
    Integer id;
    Integer ownerId;
    String ownerName;
    Integer vehicleId;
    String vehicleNumber;
    Integer warehouseId;
    String warehouseName;
    Double startLat;
    Double startLng;
    Double capacityKg;
    Double availableCapacityKg;
    LocalDateTime departureTime;
    String encodedPolyline;
    LocalDateTime estimatedArrivalTime;
    Double totalEstimatedDistanceKm;
    Boolean isShareable;
    TripStatus status;
}