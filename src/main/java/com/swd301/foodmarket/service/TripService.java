package com.swd301.foodmarket.service;

import com.swd301.foodmarket.dto.request.CreateTripRequest;
import com.swd301.foodmarket.dto.response.TripResponse;
import com.swd301.foodmarket.entity.Trip;
import com.swd301.foodmarket.enums.TripStatus;
import org.springframework.data.domain.Page;

import java.util.List;

public interface TripService {
    TripResponse createTrip(CreateTripRequest request);
    TripResponse updateTripStatus(Integer tripId, TripStatus status);
    TripResponse getTripById(Integer tripId);
    List<Trip> getShareableTrips();
    Page<TripResponse> getMyTripByStatus(int page, int size, TripStatus status);
}
