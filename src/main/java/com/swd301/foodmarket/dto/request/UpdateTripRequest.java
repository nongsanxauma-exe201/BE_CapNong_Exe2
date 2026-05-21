package com.swd301.foodmarket.dto.request;

import com.swd301.foodmarket.enums.TripStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
@Data
public class UpdateTripRequest {
    Integer vehicleId;
    Integer warehouseId;
    Double startLat;
    Double startLng;
    LocalDateTime departureTime;
    Boolean isShareable;
    Double availableCapacityKg;
    // neus isShareable = true thi moi co cai nay, neu ko thi mac dinh la 0
    Double pricePerKm;
    private List<Integer> orderRemovedIds;
    private List<Integer> orderAddedIds;

}
