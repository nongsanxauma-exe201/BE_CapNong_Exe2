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

//    @Override
//    @Transactional
//    public void calculateOptimalRoute(Trip trip) {
//        log.info("Optimizing route for trip: {}", trip.getId());
//
//        // Remove existing stops for recalculation
//        List<TripStop> existingStops = tripStopRepository.findByTripIdOrderBySequenceAsc(trip.getId());
//        tripStopRepository.deleteAll(existingStops);
//
//        int sequence = 2;
//
//
//        // 2. Add Approved Join Requests Locations (Ideally we should use GoongMapService Distance Matrix here)
//        List<TripJoinRequest> acceptedRequests = tripJoinRequestRepository.findByTripIdAndStatus(trip.getId(), TripJoinStatus.ACCEPTED);
//
//        for (TripJoinRequest req : acceptedRequests) {
//            TripStop reqStop = TripStop.builder()
//                    .trip(trip)
//                    .sequence(sequence++)
//                    .lat(req.getPickupLat())
//                    .lng(req.getPickupLng())
//                    .farmer(req.getFarmer())
//                    .stopType(TripStopType.PICKUP)
//                    .order(req.getOrders())
//                    .build();
//            tripStopRepository.save(reqStop);
//
//            for (Order order : req.getOrders()) {
//                order.setTripStop(reqStop);
//                orderRepository.save(order);
//            }
//
//        }
//
//        // 3. Add Warehouse Dropoff
//        TripStop warehouseStop = TripStop.builder()
//                .trip(trip)
//                .sequence(sequence++)
//                .lat(trip.getWarehouse().getLatitude())
//                .lng(trip.getWarehouse().getLongitude())
//                .stopType(TripStopType.WAREHOUSE_DROPOFF)
//                .build();
//        tripStopRepository.save(warehouseStop);
//
//        log.info("Route optimization completed for trip: {} with {} stops", trip.getId(), sequence - 1);
//    }

    @Transactional
    public void calculateOptimalRoute(Trip trip) {

        log.info(
                "Rebuilding route for trip: {}",
                trip.getId()
        );

        // =====================================================
        // 1. DELETE OLD STOPS
        // =====================================================

        List<TripStop> oldStops =
                tripStopRepository
                        .findByTripAndSequenceIsNot(
                                trip,
                                1
                        );
        // unlink orders before delete
        for (TripStop stop : oldStops) {

            if (stop.getOrder() != null) {

                for (Order order : stop.getOrder()) {

                    order.setTripStop(null);

                    orderRepository.save(order);
                }
            }
        }

        tripStopRepository.deleteAll(oldStops);


        // =====================================================
        // 2. CREATE NEW STOPS
        // =====================================================

        int sequence = 2;

        List<TripStop> createdStops =
                new ArrayList<>();

        // =====================================================
        // OWNER START POINT
        // =====================================================

        TripStop ownerStartStop =  tripStopRepository.findByTripAndSequence(trip, 1);
        createdStops.add(ownerStartStop);

        // =====================================================
        // APPROVED JOIN REQUESTS
        // =====================================================

        // =====================================================
// APPROVED JOIN REQUESTS
// =====================================================

        List<TripJoinRequest> remainingRequests =
                new ArrayList<>(
                        tripJoinRequestRepository
                                .findByTripIdAndStatus(
                                        trip.getId(),
                                        TripJoinStatus.ACCEPTED
                                )
                );

// current position = owner start
        double currentLat = ownerStartStop.getLat();
        double currentLng = ownerStartStop.getLng();

        while (!remainingRequests.isEmpty()) {

            TripJoinRequest nearestRequest = null;

            double nearestDistance = Double.MAX_VALUE;

            // =====================================================
            // FIND NEAREST REQUEST
            // =====================================================

            for (TripJoinRequest request : remainingRequests) {

                double distance =
                        calculateDistanceKm(
                                currentLat,
                                currentLng,
                                request.getPickupLat(),
                                request.getPickupLng()
                        );

                if (distance < nearestDistance) {

                    nearestDistance = distance;

                    nearestRequest = request;
                }
            }

            if (nearestRequest == null) {
                break;
            }

            // =====================================================
            // CREATE PICKUP STOP
            // =====================================================

            TripStop pickupStop =
                    TripStop.builder()
                            .trip(trip)
                            .sequence(sequence++)
                            .lat(nearestRequest.getPickupLat())
                            .lng(nearestRequest.getPickupLng())
                            .farmer(nearestRequest.getFarmer())
                            .stopType(TripStopType.PICKUP)
                            .order(new ArrayList<>(nearestRequest.getOrders()))
                            .priceForStop(nearestRequest.getPriceOffer())
                            .isPickup(false)
                            .build();

            pickupStop =
                    tripStopRepository.save(pickupStop);

            createdStops.add(pickupStop);

            // =====================================================
            // LINK ORDERS
            // =====================================================

            for (Order order : nearestRequest.getOrders()) {

                order.setTripStop(pickupStop);

                orderRepository.save(order);
            }

            // =====================================================
            // UPDATE CURRENT POSITION
            // =====================================================

            currentLat = nearestRequest.getPickupLat();
            currentLng = nearestRequest.getPickupLng();

            // =====================================================
            // REMOVE FROM LIST
            // =====================================================

            remainingRequests.remove(nearestRequest);
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

        for (int i = 0;i < createdStops.size() - 1; i++) {

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
    private double calculateDistanceKm(
            double lat1,
            double lng1,
            double lat2,
            double lng2
    ) {

        final int EARTH_RADIUS_KM = 6371;

        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);

        double a =
                Math.sin(dLat / 2) * Math.sin(dLat / 2)
                        + Math.cos(Math.toRadians(lat1))
                        * Math.cos(Math.toRadians(lat2))
                        * Math.sin(dLng / 2)
                        * Math.sin(dLng / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_KM * c;
    }

}
