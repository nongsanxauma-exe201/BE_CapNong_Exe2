package com.swd301.foodmarket.service;

import com.swd301.foodmarket.dto.request.CreateTripRequest;
import com.swd301.foodmarket.dto.request.UpdateTripRequest;
import com.swd301.foodmarket.dto.response.OrderResponse;
import com.swd301.foodmarket.dto.response.PageResponse;
import com.swd301.foodmarket.dto.response.TripResponse;
import com.swd301.foodmarket.dto.response.TripStopResponse;
import com.swd301.foodmarket.entity.Trip;
import com.swd301.foodmarket.entity.TripStop;
import com.swd301.foodmarket.enums.TripStatus;
import org.springframework.data.domain.Page;

import java.util.List;

public interface TripService {
    TripResponse createTrip(CreateTripRequest request);
    TripResponse updateTripStatus(Integer tripId, TripStatus status);
    TripResponse updateTrip(Integer tripId, UpdateTripRequest request);
    TripResponse getTripById(Integer tripId);
    List<Trip> getShareableTrips();
    Page<TripResponse> getMyTripByStatus(int page, int size, TripStatus status);
    Page<OrderResponse> getOrdersForTrip(int page, int size);
    PageResponse<TripResponse> getTripJoinedByCurrentUser(int page, int size, TripStatus status);
    TripStopResponse updateTripStopToPicked(Integer stopId);
}
