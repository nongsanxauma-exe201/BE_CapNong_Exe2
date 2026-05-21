package com.swd301.foodmarket.service;

import com.swd301.foodmarket.dto.response.TripResponse;
import com.swd301.foodmarket.entity.Trip;
import com.swd301.foodmarket.dto.goong.LatLong;

import java.time.LocalDateTime;
import java.util.List;

public interface TripMatchingService {
    List<TripResponse> findSuggestedTrips(LatLong pickupLoc, Integer warehouseId);
}
