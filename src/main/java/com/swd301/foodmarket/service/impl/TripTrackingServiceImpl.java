package com.swd301.foodmarket.service.impl;


import com.swd301.foodmarket.dto.request.TripTrackingRequest;
import com.swd301.foodmarket.dto.response.TripTrackingResponse;
import com.swd301.foodmarket.entity.Trip;
import com.swd301.foodmarket.entity.TripTracking;
import com.swd301.foodmarket.entity.User;
import com.swd301.foodmarket.repository.TripRepository;
import com.swd301.foodmarket.repository.TripTrackingRepository;
import com.swd301.foodmarket.repository.UserRepository;
import com.swd301.foodmarket.service.TripTrackingService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class TripTrackingServiceImpl
        implements TripTrackingService {

    private final TripTrackingRepository
            tripTrackingRepository;

    private final TripRepository
            tripRepository;

    private final UserRepository
            userRepository;

    @Override
    @Transactional
    public TripTrackingResponse updateLocation(
            TripTrackingRequest request,
            String email
    ) {

        User user =  userRepository.findByEmail(email)
                        .orElseThrow(() -> new RuntimeException("User not found"  ) );
        Trip trip =tripRepository.findById( request.getTripId() ) .orElseThrow(() -> new RuntimeException("Trip not found" ));
        if (!trip.getOwner().getId().equals(user.getId())) {
            throw new RuntimeException("Only trip owner can update GPS");}
        TripTracking tracking =
                TripTracking.builder()
                        .trip(trip)
                        .lat(request.getLat())
                        .lng(request.getLng())
                        .speed(request.getSpeed())
                        .heading(request.getHeading())
                        .createdAt(LocalDateTime.now())
                        .build();

        tripTrackingRepository.save(tracking);
        return TripTrackingResponse.builder()
                .tripId(trip.getId())
                .lat(tracking.getLat())
                .lng(tracking.getLng())
                .speed(tracking.getSpeed())
                .heading(tracking.getHeading())
                .updatedAt(
                        tracking.getCreatedAt()
                                .toString()
                )
                .build();
    }
}
