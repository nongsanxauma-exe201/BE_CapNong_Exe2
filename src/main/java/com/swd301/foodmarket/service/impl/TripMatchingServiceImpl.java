package com.swd301.foodmarket.service.impl;

import com.swd301.foodmarket.dto.goong.LatLong;
import com.swd301.foodmarket.dto.response.TripResponse;
import com.swd301.foodmarket.entity.Trip;
import com.swd301.foodmarket.mapper.TripMapper;
import com.swd301.foodmarket.service.GoongMapService;
import com.swd301.foodmarket.service.TripMatchingService;
import com.swd301.foodmarket.service.TripService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TripMatchingServiceImpl implements TripMatchingService {

    private final TripService tripService;
    private final GoongMapService goongMapService;
    private final TripMapper tripMapper;

    @Override
    public List<TripResponse> findSuggestedTrips(LatLong pickupLoc, Integer warehouseId) {
        List<Trip> availableTrips = tripService.getShareableTrips();

        return availableTrips.stream().filter(trip -> {
//            // Check capacity
//            if (trip.getAvailableCapacityKg() < requestedWeight) return false;
            
            // Check warehouse destination (simplified)
            if (!trip.getWarehouse().getId().equals(warehouseId)) return false;

            // Check if the pickup location is near the route
            LatLong tripStartLoc = new LatLong(trip.getStartLat(), trip.getStartLng());
            LatLong warehouseLoc = new LatLong(trip.getWarehouse().getLatitude(), trip.getWarehouse().getLongitude());

            double distStartToShop = goongMapService.getDistanceKm(tripStartLoc, pickupLoc);
            if (distStartToShop <= 5.0) return true; // Very near the start point

            double distShopToWarehouse = goongMapService.getDistanceKm(pickupLoc, warehouseLoc);
            double distStartToWarehouse = goongMapService.getDistanceKm(tripStartLoc, warehouseLoc);

            // Calculate "detour" distance: (Start -> Shop) + (Shop -> Warehouse) - (Start -> Warehouse)
            // If this detour is small (e.g., <= 5km), it means the shop is roughly along the route
            double detour = (distStartToShop + distShopToWarehouse) - distStartToWarehouse;
            return detour <= 5.0;
        }).collect(Collectors.toList()).stream().map(tripMapper::entityToResponse).toList();
    }
}
