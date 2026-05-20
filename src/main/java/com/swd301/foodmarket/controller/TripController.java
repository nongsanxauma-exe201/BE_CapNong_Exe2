package com.swd301.foodmarket.controller;

import com.swd301.foodmarket.dto.goong.LatLong;
import com.swd301.foodmarket.dto.request.CreateTripJoinRequest;
import com.swd301.foodmarket.dto.request.CreateTripRequest;
import com.swd301.foodmarket.dto.response.ApiResponse;
import com.swd301.foodmarket.dto.response.TripJoinRequestResponse;
import com.swd301.foodmarket.dto.response.TripResponse;
import com.swd301.foodmarket.entity.Trip;
import com.swd301.foodmarket.entity.TripJoinRequest;
import com.swd301.foodmarket.enums.TripStatus;
import com.swd301.foodmarket.service.TripJoinService;
import com.swd301.foodmarket.service.TripMatchingService;
import com.swd301.foodmarket.service.TripService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/trips")
@RequiredArgsConstructor
public class TripController {

    private final TripService tripService;
    private final TripMatchingService tripMatchingService;
    private final TripJoinService tripJoinService;
    @PostMapping
    public ResponseEntity<ApiResponse<TripResponse>> createTrip(
            @RequestBody CreateTripRequest trip
    ) {

        return ResponseEntity.ok(
                ApiResponse.<TripResponse>builder()
                        .result(tripService.createTrip(trip))
                        .build()
        );
    }

    @GetMapping("/{tripId}")
    public ResponseEntity<ApiResponse<TripResponse>> getTripById(
            @PathVariable Integer tripId
    ) {

        return ResponseEntity.ok(
                ApiResponse.<TripResponse>builder()
                        .result(tripService.getTripById(tripId))
                        .build()
        );
    }

    @PatchMapping("/{tripId}/status")
    public ResponseEntity<ApiResponse<TripResponse>> updateTripStatus(
            @PathVariable Integer tripId,
            @RequestParam TripStatus status
    ) {

        return ResponseEntity.ok(
                ApiResponse.<TripResponse>builder()
                        .result(
                                tripService.updateTripStatus(
                                        tripId,
                                        status
                                )
                        )
                        .build()
        );
    }

    @GetMapping("/suggested")
    public ResponseEntity<ApiResponse<List<TripResponse>>> getSuggestedTrips(
            @RequestParam Double lat,
            @RequestParam Double lng,
            @RequestParam Integer warehouseId,
            @RequestParam Double requestedWeight,
            @RequestParam String departureTimeStr
    ) {

        LatLong pickupLoc = new LatLong(lat, lng);

        LocalDateTime departureTime =
                LocalDateTime.parse(departureTimeStr);

        return ResponseEntity.ok(
                ApiResponse.<List<TripResponse>>builder()
                        .result(
                                tripMatchingService.findSuggestedTrips(
                                        pickupLoc,
                                        warehouseId,
                                        requestedWeight,
                                        departureTime
                                )
                        )
                        .build()
        );
    }

    @PostMapping("/{tripId}/join")
    public ResponseEntity<ApiResponse<TripJoinRequestResponse>> requestToJoin(
            @PathVariable Integer tripId,
            @RequestBody CreateTripJoinRequest request
    ) {

        return ResponseEntity.ok(
                ApiResponse.<TripJoinRequestResponse>builder()
                        .result(
                                tripJoinService.requestToJoin(
                                        tripId,
                                        request
                                )
                        )
                        .build()
        );
    }

    @PostMapping("/requests/{requestId}/approve")
    public ResponseEntity<ApiResponse<TripJoinRequestResponse>> approveJoinRequest(
            @PathVariable Integer requestId
    ) {

        return ResponseEntity.ok(
                ApiResponse.<TripJoinRequestResponse>builder()
                        .result(
                                tripJoinService.approveJoinRequest(
                                        requestId
                                )
                        )
                        .build()
        );
    }

    @PostMapping("/requests/{requestId}/reject")
    public ResponseEntity<ApiResponse<TripJoinRequestResponse>> rejectJoinRequest(
            @PathVariable Integer requestId,
            @RequestParam Integer ownerId
    ) {
        return ResponseEntity.ok(
                ApiResponse.<TripJoinRequestResponse>builder()
                        .result(
                                tripJoinService.rejectJoinRequest(
                                        requestId
                                )
                        )
                        .build()
        );
    }
}
