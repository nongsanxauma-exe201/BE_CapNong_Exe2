package com.swd301.foodmarket.dto.request;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CreateTripRequest {
    Integer vehicleId;
    Integer warehouseId;
    Double startLat;
    Double startLng;
    LocalDateTime departureTime;
    Boolean isShareable;
    Double availableCapacityKg;
    // neus isShareable = true thi moi co cai nay, neu ko thi mac dinh la 0
    Double basePrice;
    Double distanceRate;
    Double weightRate;
    // order cua minh
    List<Integer> orderIds;
}
