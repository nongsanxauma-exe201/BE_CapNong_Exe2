package com.swd301.foodmarket.controller;

import com.swd301.foodmarket.dto.goong.LatLong;
import com.swd301.foodmarket.dto.request.CreateTripJoinRequest;
import com.swd301.foodmarket.dto.request.CreateTripRequest;
import com.swd301.foodmarket.dto.response.*;
import com.swd301.foodmarket.entity.Trip;
import com.swd301.foodmarket.entity.TripJoinRequest;
import com.swd301.foodmarket.enums.TripStatus;
import com.swd301.foodmarket.service.TripJoinService;
import com.swd301.foodmarket.service.TripMatchingService;
import com.swd301.foodmarket.service.TripService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
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
    @GetMapping("/orders-for-trip")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getOrdersForTrip(
            @RequestParam(required = false,defaultValue = "0") int page,
            @RequestParam(required = false,defaultValue = "10")  int size
    ) {
        return ResponseEntity.ok(
                ApiResponse.<Page<OrderResponse>>builder()
                        .result(tripService.getOrdersForTrip(page, size))
                        .build()
        );
    }
    @GetMapping("/my-trips")
    public ResponseEntity<ApiResponse<Page<TripResponse>>> getMyTrips(
            @RequestParam(required = false,defaultValue = "0") int page,
            @RequestParam(required = false,defaultValue = "10")  int size,
            @RequestParam(required = false) TripStatus status
    ) {
        return ResponseEntity.ok(
                ApiResponse.<Page<TripResponse>>builder()
                        .result(tripService.getMyTripByStatus(page, size,status))
                        .build()
        );
    }
    @GetMapping("/trips-joined")
    public ResponseEntity<ApiResponse<PageResponse<TripResponse>>> getTripJoined(
            @RequestParam(required = false,defaultValue = "0") int page,
            @RequestParam(required = false,defaultValue = "10")  int size,
            @RequestParam(required = false) TripStatus status
    ) {
        return ResponseEntity.ok(
                ApiResponse.<PageResponse<TripResponse>>builder()
                        .result(tripService.getTripJoinedByCurrentUser(page, size,status))
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
            @RequestParam Integer warehouseId
    ) {

        LatLong pickupLoc = new LatLong(lat, lng);

        return ResponseEntity.ok(
                ApiResponse.<List<TripResponse>>builder()
                        .result(
                                tripMatchingService.findSuggestedTrips(
                                        pickupLoc,
                                        warehouseId

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

    @GetMapping("/join-requests")
    public ResponseEntity<ApiResponse<Page<TripJoinRequestResponse>>> getAllSendRequest(
            @RequestParam(required = false,defaultValue = "0") int page,
            @RequestParam(required = false,defaultValue = "10")  int size
    ) {
        return ResponseEntity.ok(
                ApiResponse.<Page<TripJoinRequestResponse>>builder()
                        .result(
                                tripJoinService.getMySentJoinRequests(page, size)
                        )
                        .build()
        );

    }
    @GetMapping("/requests-recived")
    public ResponseEntity<ApiResponse<Page<TripJoinRequestResponse>>> getAllRequestForMyTrips(
            @RequestParam(required = false,defaultValue = "0") int page,
            @RequestParam(required = false,defaultValue = "10")  int size
    ) {
        return ResponseEntity.ok(
                ApiResponse.<Page<TripJoinRequestResponse>>builder()
                        .result(
                                tripJoinService.getJoinRequestsForMyTrips(page, size)
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
            @PathVariable Integer requestId
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
    @GetMapping("/join-requests/{tripId}")
    public ResponseEntity<ApiResponse<TripJoinRequestResponse>> getTripJoinedById(
            @PathVariable Integer tripId
    ) {
        return ResponseEntity.ok(
                ApiResponse.<TripJoinRequestResponse>builder()
                        .result(tripJoinService.getJoinRequestById(tripId))
                        .build()
        );
    }
    @PutMapping("/stops/{stopId}/picked")
    public ResponseEntity<ApiResponse<TripStopResponse>> updateTripStopToPicked(
            @PathVariable Integer stopId
    ) {
        return ResponseEntity.ok(
                ApiResponse.<TripStopResponse>builder()
                        .result(tripService.updateTripStopToPicked(stopId))
                        .build()
        );
    }
}
