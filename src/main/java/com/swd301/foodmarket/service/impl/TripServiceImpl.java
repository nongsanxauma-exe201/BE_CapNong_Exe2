package com.swd301.foodmarket.service.impl;

import com.swd301.foodmarket.dto.goong.LatLong;
import com.swd301.foodmarket.dto.request.CreateTripRequest;
import com.swd301.foodmarket.dto.response.OsrmRouteResponse;
import com.swd301.foodmarket.dto.response.RouteData;
import com.swd301.foodmarket.dto.response.TripResponse;
import com.swd301.foodmarket.entity.Trip;
import com.swd301.foodmarket.entity.User;
import com.swd301.foodmarket.entity.Vehicle;
import com.swd301.foodmarket.entity.Warehouse;
import com.swd301.foodmarket.enums.TripStatus;
import com.swd301.foodmarket.exception.AppException;
import com.swd301.foodmarket.exception.ErrorCode;
import com.swd301.foodmarket.mapper.TripMapper;
import com.swd301.foodmarket.repository.TripRepository;
import com.swd301.foodmarket.repository.UserRepository;
import com.swd301.foodmarket.repository.VehicleRepository;
import com.swd301.foodmarket.repository.WarehouseRepository;
import com.swd301.foodmarket.service.GoongMapService;
import com.swd301.foodmarket.service.RoutingService;
import com.swd301.foodmarket.service.TripService;
import com.swd301.foodmarket.service.WarehouseService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TripServiceImpl implements TripService {

    private final TripRepository tripRepository;
    private final WarehouseRepository warehouseRepository;
    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;
    private final TripMapper tripMapper;
    private final GoongMapService goongMapService;
    private final RoutingService routingService;

    @Override
    @Transactional
    public TripResponse createTrip(CreateTripRequest request) {
        User owner = getCurrentUser();

        Vehicle vehicle = vehicleRepository.findById(request.getVehicleId()).orElseThrow(
                () -> new AppException(ErrorCode.VEHICLE_NOT_FOUND));
        Warehouse warehouse = warehouseRepository.findById(request.getWarehouseId()).orElseThrow(
                () -> new AppException(ErrorCode.WAREHOUSE_NOT_FOUND));
        Trip trip = tripMapper.requestToEntity(request);
        trip.setWarehouse(warehouse);
        trip.setVehicle(vehicle);
        trip.setOwner(owner);

        ///  mỗi khi mà có người được duyệt gheps thì cập nhật lại

        // tinh toán lại thời gian dự kiến đến nơi
        // va duong di
        RouteData route =
                routingService.getRoute(
                        request.getStartLat(),
                        request.getStartLng(),
                        warehouse.getLatitude(),
                        warehouse.getLongitude()
                );
        trip.setEncodedPolyline(route.getPolyline());
        trip.setEstimatedArrivalTime(LocalDateTime.from(request.getDepartureTime()).plusMinutes((long) (route.getDurationSeconds() / 60)));
        trip.setTotalEstimatedDistanceKm(route.getDistanceKm());
        tripRepository.save(trip);
        return tripMapper.entityToResponse(trip);
    }

    @Override
    @Transactional
    public TripResponse updateTripStatus(Integer tripId, TripStatus status) {
        Trip trip = tripRepository.findById(tripId).orElseThrow(
                () -> new AppException(ErrorCode.TRIP_NOT_FOUND)
        );
        trip.setStatus(status);
        return tripMapper.entityToResponse(trip);
    }

    @Override
    public TripResponse getTripById(Integer tripId) {


        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new AppException(ErrorCode.TRIP_NOT_FOUND));
        return tripMapper.entityToResponse(trip);
    }

    @Override
    public List<Trip> getShareableTrips() {
//
//        Pageable pageable = PageRequest.of(page, size, Sort.by("departureTime").ascending());
        return tripRepository.findByIsShareableAndDepartureTimeIsAfter(true, LocalDateTime.now());

    }

    @Override
    public Page<TripResponse> getMyTripByStatus(int page, int size, TripStatus status) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("departureTime").descending());
        Page<Trip> tripPage = tripRepository.findByStatus(status, pageable);
        return tripPage.map(tripMapper::entityToResponse);
    }

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }
}
