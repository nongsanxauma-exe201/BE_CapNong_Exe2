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
    public void calculateOptimalRoute(Trip trip) {
        log.info("Rebuilding TRULY OPTIMAL route (TSP) for trip: {}", trip.getId());

        // =====================================================
        // 1. DELETE OLD STOPS & UNLINK ORDERS
        // =====================================================
        List<TripStop> oldStops = tripStopRepository.findByTripAndSequenceIsNot(trip, 1);
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
        // 2. GET START POINT & REQUESTS
        // =====================================================
        TripStop ownerStartStop = tripStopRepository.findByTripAndSequence(trip, 1);
        if (ownerStartStop == null) {
            log.error("Start stop (sequence 1) not found for trip: {}", trip.getId());
            return;
        }

        List<TripJoinRequest> acceptedRequests = tripJoinRequestRepository
                .findByTripIdAndStatus(trip.getId(), TripJoinStatus.ACCEPTED);

        // =====================================================
        // 3. TSP OPTIMIZATION (Find the absolute best permutation)
        // =====================================================
        List<TripJoinRequest> optimalPermutation = findOptimalTSPRoute(ownerStartStop, trip, acceptedRequests);

        // =====================================================
        // 4. CREATE NEW STOPS IN OPTIMAL ORDER
        // =====================================================
        int sequence = 2;
        List<TripStop> createdStops = new ArrayList<>();
        createdStops.add(ownerStartStop);

        // Create Pickup Stops
        for (TripJoinRequest request : optimalPermutation) {
            TripStop pickupStop = TripStop.builder()
                    .trip(trip)
                    .sequence(sequence++)
                    .lat(request.getPickupLat())
                    .lng(request.getPickupLng())
                    .farmer(request.getFarmer())
                    .stopType(TripStopType.PICKUP)
                    .order(new ArrayList<>(request.getOrders()))
                    .priceForStop(request.getPriceOffer())
                    .isPickup(true) // Đổi thành true vì đây là điểm Pickup
                    .build();

            pickupStop = tripStopRepository.save(pickupStop);
            createdStops.add(pickupStop);

            // Link orders
            for (Order order : request.getOrders()) {
                order.setTripStop(pickupStop);
                orderRepository.save(order);
            }
        }

        // Create Warehouse Stop
        TripStop warehouseStop = TripStop.builder()
                .trip(trip)
                .sequence(sequence++)
                .lat(trip.getWarehouse().getLatitude())
                .lng(trip.getWarehouse().getLongitude())
                .stopType(TripStopType.WAREHOUSE_DROPOFF)
                .isPickup(false)
                .build();

        warehouseStop = tripStopRepository.save(warehouseStop);
        createdStops.add(warehouseStop);

        // =====================================================
        // 5. CALCULATE TOTAL DISTANCE, ETA & ENCODED POLYLINE
        // =====================================================
        double totalDistanceKm = 0;
        long totalDurationSeconds = 0;
        StringBuilder fullPolyline = new StringBuilder();

        // Đi qua từng cặp điểm để gọi API Routing thực tế lấy Distance và Duration
        LocalDateTime currentEta = trip.getDepartureTime();
        createdStops.get(0).setActualArrivalTime(currentEta);

        for (int i = 0; i < createdStops.size() - 1; i++) {
            TripStop current = createdStops.get(i);
            TripStop next = createdStops.get(i + 1);

            RouteData routeData = routingService.getRoute(
                    current.getLat(), current.getLng(),
                    next.getLat(), next.getLng()
            );

            if (routeData != null) {
                totalDistanceKm += routeData.getDistanceKm();
                totalDurationSeconds += routeData.getDurationSeconds();

                currentEta = currentEta.plusSeconds(routeData.getDurationSeconds());
                next.setActualArrivalTime(currentEta);
                tripStopRepository.save(next);

                // Bạn có thể nối chuỗi polyline ở đây nếu hệ thống map hỗ trợ vẽ nhiều chặng
                if (i == 0) {
                    trip.setEncodedPolyline(routeData.getPolyline());
                }
            } else {
                // Fallback tính chim bay nếu API map lỗi
                double fallbackDist = calculateDistanceKm(current.getLat(), current.getLng(), next.getLat(), next.getLng());
                totalDistanceKm += fallbackDist;
                currentEta = currentEta.plusMinutes(15); // tạm tính 15p cho mỗi chặng lỗi
                next.setActualArrivalTime(currentEta);
                tripStopRepository.save(next);
            }
        }

        // =====================================================
        // 6. UPDATE TRIP INFO
        // =====================================================
        trip.setTotalEstimatedDistanceKm(totalDistanceKm);
        trip.setEstimatedArrivalTime(trip.getDepartureTime().plusSeconds(totalDurationSeconds));
        tripRepository.save(trip);

        log.info("Route optimized successfully via TSP for trip: {} with {} stops. Total Dist: {} km",
                trip.getId(), createdStops.size(), totalDistanceKm);
    }

    /**
     * Hàm tìm chuỗi lộ trình tối ưu nhất bằng thuật toán hoán vị (TSP)
     */
    private List<TripJoinRequest> findOptimalTSPRoute(TripStop start, Trip trip, List<TripJoinRequest> requests) {
        if (requests.isEmpty()) return requests;

        // 1. Sinh tất cả các hoán vị của danh sách requests
        List<List<TripJoinRequest>> allPermutations = new ArrayList<>();
        generatePermutations(requests, 0, allPermutations);

        List<TripJoinRequest> bestRoute = null;
        double minTotalDistance = Double.MAX_VALUE;

        // 2. Duyệt qua từng kịch bản để tìm lộ trình có TỔNG KHOẢNG CÁCH THỰC TẾ ngắn nhất
        for (List<TripJoinRequest> permutation : allPermutations) {
            double currentDistance = 0;

            // Điểm bắt đầu (Điểm đầu của chủ xe)
            double currentLat = start.getLat();
            double currentLng = start.getLng();

            boolean isValidRoute = true;

            // Tính qua từng điểm Pickup trong hoán vị hiện tại
            for (TripJoinRequest req : permutation) {
                // Gọi Routing Service để lấy khoảng cách thực tế trên đường đi thay vì chim bay
                RouteData data = routingService.getRoute(currentLat, currentLng, req.getPickupLat(), req.getPickupLng());
                if (data != null) {
                    currentDistance += data.getDistanceKm();
                } else {
                    // Fallback sang chim bay nếu API lỗi
                    currentDistance += calculateDistanceKm(currentLat, currentLng, req.getPickupLat(), req.getPickupLng());
                }

                currentLat = req.getPickupLat();
                currentLng = req.getPickupLng();
            }

            // Chặng cuối cùng: Từ điểm Pickup cuối cùng về Warehouse (Kho)
            RouteData finalData = routingService.getRoute(currentLat, currentLng, trip.getWarehouse().getLatitude(), trip.getWarehouse().getLongitude());
            if (finalData != null) {
                currentDistance += finalData.getDistanceKm();
            } else {
                currentDistance += calculateDistanceKm(currentLat, currentLng, trip.getWarehouse().getLatitude(), trip.getWarehouse().getLongitude());
            }

            // Cập nhật lộ trình tốt nhất tuyệt đối
            if (currentDistance < minTotalDistance) {
                minTotalDistance = currentDistance;
                bestRoute = new ArrayList<>(permutation);
            }
        }

        return bestRoute;
    }

    /**
     * Hàm sinh hoán vị (Backtracking)
     */
    private void generatePermutations(List<TripJoinRequest> list, int index, List<List<TripJoinRequest>> result) {
        if (index >= list.size() - 1) {
            result.add(new ArrayList<>(list));
            return;
        }
        for (int i = index; i < list.size(); i++) {
            swap(list, index, i);
            generatePermutations(list, index + 1, result);
            swap(list, index, i); // backtrack
        }
    }

    private void swap(List<TripJoinRequest> list, int i, int j) {
        TripJoinRequest temp = list.get(i);
        list.set(i, list.get(j));
        list.set(j, temp);
    }

    private double calculateDistanceKm(double lat1, double lng1, double lat2, double lng2) {
        final int EARTH_RADIUS_KM = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }
}
