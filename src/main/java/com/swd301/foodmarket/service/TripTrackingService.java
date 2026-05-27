package com.swd301.foodmarket.service;

import com.swd301.foodmarket.dto.request.TripTrackingRequest;
import com.swd301.foodmarket.dto.response.TripTrackingResponse;

public interface TripTrackingService {
    TripTrackingResponse  updateLocation(
                           TripTrackingRequest tripTrackingRequest,
                            String email
                    );
}
