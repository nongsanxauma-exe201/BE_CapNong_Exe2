package com.swd301.foodmarket.service;

import com.swd301.foodmarket.dto.request.CreateTripJoinRequest;
import com.swd301.foodmarket.dto.response.TripJoinRequestResponse;
import com.swd301.foodmarket.entity.TripJoinRequest;

public interface TripJoinService {
    TripJoinRequestResponse requestToJoin( Integer tripId,CreateTripJoinRequest request);
    TripJoinRequestResponse approveJoinRequest(Integer requestId);
    TripJoinRequestResponse rejectJoinRequest(Integer requestId);
}
