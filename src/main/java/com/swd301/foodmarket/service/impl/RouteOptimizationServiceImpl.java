package com.swd301.foodmarket.service.impl;

import com.swd301.foodmarket.dto.response.RouteData;
import com.swd301.foodmarket.entity.Order;
import com.swd301.foodmarket.entity.Trip;
import com.swd301.foodmarket.entity.TripJoinRequest;
import com.swd301.foodmarket.entity.TripStop;
import com.swd301.foodmarket.enums.TripJoinStatus;
import com.swd301.foodmarket.enums.TripStopType;
import com.swd301.foodmarket.repository.OrderRepository;
import com.swd301.foodmarket.repository.TripJoinRequestRepository;
import com.swd301.foodmarket.repository.TripRepository;
import com.swd301.foodmarket.repository.TripStopRepository;
import com.swd301.foodmarket.service.RouteOptimizationService;
import com.swd301.foodmarket.service.RoutingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RouteOptimizationServiceImpl implements RouteOptimizationService {

    private final TripStopRepository tripStopRepository;
    private final TripJoinRequestRepository tripJoinRequestRepository;
    private final OrderRepository orderRepository;
    private final RoutingService routingService;
    private final TripRepository tripRepository;

    @Override
    @Transactional
    public void calculateOptimalRoute(Trip trip) {
        log.info("Optimizing route for trip: {}", trip.getId());

        // Remove existing stops for recalculation
        List<TripStop> existingStops = tripStopRepository.findByTripIdOrderBySequenceAsc(trip.getId());
        tripStopRepository.deleteAll(existingStops);

        int sequence = 1;

        // 1. Add Trip Owner's Start Location
        TripStop ownerStop = TripStop.builder()
                .trip(trip)
                .sequence(sequence++)
                .lat(trip.getStartLat())
                .lng(trip.getStartLng())
                .farmer(trip.getOwner())
                .stopType(TripStopType.PICKUP)
                .build();
        tripStopRepository.save(ownerStop);

        // 2. Add Approved Join Requests Locations (Ideally we should use GoongMapService Distance Matrix here)
        List<TripJoinRequest> acceptedRequests = tripJoinRequestRepository.findByTripIdAndStatus(trip.getId(), TripJoinStatus.ACCEPTED);

        for (TripJoinRequest req : acceptedRequests) {
            TripStop reqStop = TripStop.builder()
                    .trip(trip)
                    .sequence(sequence++)
                    .lat(req.getPickupLat())
                    .lng(req.getPickupLng())
                    .farmer(req.getFarmer())
                    .stopType(TripStopType.PICKUP)
                    .order(req.getOrders())
                    .build();
            tripStopRepository.save(reqStop);

            for (Order order : req.getOrders()) {
                order.setTripStop(reqStop);
                orderRepository.save(order);
            }

        }

        // 3. Add Warehouse Dropoff
        TripStop warehouseStop = TripStop.builder()
                .trip(trip)
                .sequence(sequence++)
                .lat(trip.getWarehouse().getLatitude())
                .lng(trip.getWarehouse().getLongitude())
                .stopType(TripStopType.WAREHOUSE_DROPOFF)
                .build();
        tripStopRepository.save(warehouseStop);

        log.info("Route optimization completed for trip: {} with {} stops", trip.getId(), sequence - 1);
    }

    @Transactional
    public void calculateOptimalRoute2(Trip trip) {

        log.info(
                "Rebuilding route for trip: {}",
                trip.getId()
        );

        // =====================================================
        // 1. DELETE OLD STOPS
        // =====================================================

        List<TripStop> oldStops =
                tripStopRepository
                        .findByTripIdOrderBySequenceAsc(
                                trip.getId()
                        );

        tripStopRepository.deleteAll(oldStops);

        // =====================================================
        // 2. CREATE NEW STOPS
        // =====================================================

        int sequence = 1;

        List<TripStop> createdStops =
                new ArrayList<>();

        // =====================================================
        // OWNER START POINT
        // =====================================================

        TripStop ownerStartStop = TripStop.builder()
                .trip(trip)
                .sequence(sequence++)
                .lat(trip.getStartLat())
                .lng(trip.getStartLng())
                .farmer(trip.getOwner())
                .stopType(TripStopType.PICKUP)
                .isPickup(false)
                .build();

        ownerStartStop =
                tripStopRepository.save(ownerStartStop);

        createdStops.add(ownerStartStop);

        // =====================================================
        // APPROVED JOIN REQUESTS
        // =====================================================

        List<TripJoinRequest> approvedRequests =
                tripJoinRequestRepository
                        .findByTripIdAndStatus(
                                trip.getId(),
                                TripJoinStatus.ACCEPTED
                        );

        for (TripJoinRequest request
                : approvedRequests) {

            TripStop pickupStop =
                    TripStop.builder()
                            .trip(trip)
                            .sequence(sequence++)
                            .lat(request.getPickupLat())
                            .lng(request.getPickupLng())
                            .farmer(request.getFarmer())
                            .stopType(
                                    TripStopType.PICKUP
                            )
                            .order(request.getOrders())
                            .isPickup(false)
                            .build();

            pickupStop =
                    tripStopRepository.save(pickupStop);

            createdStops.add(pickupStop);

            // =============================================
            // LINK ORDERS TO STOP
            // =============================================

            for (Order order
                    : request.getOrders()) {

                order.setTripStop(pickupStop);

                orderRepository.save(order);
            }
        }

        // =====================================================
        // WAREHOUSE DROPOFF
        // =====================================================

        TripStop warehouseStop =
                TripStop.builder()
                        .trip(trip)
                        .sequence(sequence++)
                        .lat(
                                trip.getWarehouse()
                                        .getLatitude()
                        )
                        .lng(
                                trip.getWarehouse()
                                        .getLongitude()
                        )
                        .stopType(
                                TripStopType.WAREHOUSE_DROPOFF
                        )
                        .isPickup(false)
                        .build();

        warehouseStop =
                tripStopRepository.save(warehouseStop);

        createdStops.add(warehouseStop);

        // =====================================================
        // 3. CALCULATE TOTAL DISTANCE + ETA
        // =====================================================

        double totalDistanceKm = 0;

        long totalDurationSeconds = 0;

        for (int i = 0;
             i < createdStops.size() - 1;
             i++) {

            TripStop current =
                    createdStops.get(i);

            TripStop next =
                    createdStops.get(i + 1);

            RouteData routeData =
                    routingService.getRoute(
                            current.getLat(),
                            current.getLng(),
                            next.getLat(),
                            next.getLng()
                    );

            if (routeData != null) {

                totalDistanceKm +=
                        routeData.getDistanceKm();

                totalDurationSeconds +=
                        routeData.getDurationSeconds();

                // Save polyline only once
                if (trip.getEncodedPolyline() == null) {

                    trip.setEncodedPolyline(
                            routeData.getPolyline()
                    );
                }
            }
        }

        // =====================================================
        // 4. UPDATE TRIP INFO
        // =====================================================

        trip.setTotalEstimatedDistanceKm(
                totalDistanceKm
        );

        trip.setEstimatedArrivalTime(
                trip.getDepartureTime()
                        .plusSeconds(
                                totalDurationSeconds
                        )
        );

        // =====================================================
        // 5. CALCULATE ETA EACH STOP
        // =====================================================

        LocalDateTime currentEta =
                trip.getDepartureTime();

        createdStops.get(0)
                .setActualArrivalTime(currentEta);

        for (int i = 1;  i < createdStops.size(); i++) {
            TripStop previous =
                    createdStops.get(i - 1);

            TripStop current =
                    createdStops.get(i);

            RouteData routeData =
                    routingService.getRoute(
                            previous.getLat(),
                            previous.getLng(),
                            current.getLat(),
                            current.getLng()
                    );

            if (routeData != null) {
                currentEta =
                        currentEta.plusSeconds(
                                routeData.getDurationSeconds()
                        );
            }

            current.setActualArrivalTime(currentEta);

            tripStopRepository.save(current);
        }

        // =====================================================
        // 6. SAVE TRIP
        // =====================================================

        tripRepository.save(trip);

        log.info(
                "Route rebuilt successfully for trip: {} with {} stops",
                trip.getId(),
                createdStops.size()
        );
    }

}
