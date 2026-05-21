package com.swd301.foodmarket.service.impl;

import com.swd301.foodmarket.dto.goong.LatLong;
import com.swd301.foodmarket.dto.request.CreateTripRequest;
import com.swd301.foodmarket.dto.request.UpdateTripRequest;
import com.swd301.foodmarket.dto.response.*;
import com.swd301.foodmarket.entity.*;
import com.swd301.foodmarket.enums.OrderStatus;
import com.swd301.foodmarket.enums.TripJoinStatus;
import com.swd301.foodmarket.enums.TripStatus;
import com.swd301.foodmarket.enums.TripStopType;
import com.swd301.foodmarket.exception.AppException;
import com.swd301.foodmarket.exception.ErrorCode;
import com.swd301.foodmarket.mapper.OrderMapper;
import com.swd301.foodmarket.mapper.TripMapper;
import com.swd301.foodmarket.mapper.TripStropMapper;
import com.swd301.foodmarket.repository.*;
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    private final TripStopRepository tripStopRepository;
    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final TripJoinRequestRepository tripJoinRequestRepository;
    private final TripStropMapper tripStropMapper;

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
        trip.setCapacityKg(vehicle.getCapacityKg());
        tripRepository.save(trip);
// trip có 1 stop mặc định là stop của chủ trip, sau này sẽ có thêm stop của các shop khi được duyệt gheps
        TripStop ownerStop = TripStop.builder()
                .trip(trip)
                .sequence(1)
                .lat(trip.getStartLat())
                .lng(trip.getStartLng())
                .farmer(trip.getOwner())
                .stopType(TripStopType.PICKUP)
                .build();
        tripStopRepository.save(ownerStop);
        updateTripAndOrder(ownerStop, request.getOrderIds());
        //

        return tripMapper.entityToResponse(trip);
    }

    public void updateTripAndOrder(TripStop tripStop, List<Integer> orderIds) {
        List<Order> orders = new ArrayList<>();
        for (Integer orderId : orderIds) {
            Order order = orderRepository.findById(orderId).orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
            order.setTripStop(tripStop);
            order.setStatus(OrderStatus.SHIPPING_TO_WAREHOUSE);
            orders.add(order);

        }
        tripStop.setOrder(orders);
        tripStopRepository.save(tripStop);
        orderRepository.saveAll(orders);

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
    public TripResponse updateTrip(Integer tripId, UpdateTripRequest request) {
        User owner = getCurrentUser();

        Vehicle vehicle = vehicleRepository.findById(request.getVehicleId()).orElseThrow(
                () -> new AppException(ErrorCode.VEHICLE_NOT_FOUND));

        Warehouse warehouse = warehouseRepository.findById(request.getWarehouseId()).orElseThrow(
                () -> new AppException(ErrorCode.WAREHOUSE_NOT_FOUND));
        Trip trip = tripMapper.updateEntityFromRequest(request);
        trip.setWarehouse(warehouse);
        trip.setVehicle(vehicle);
        trip.setOwner(owner);

        return null;
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
        return tripRepository.findByIsShareableAndDepartureTimeIsAfterAndStatusAndOwnerIsNot(true, LocalDateTime.now(),TripStatus.CREATED, getCurrentUser());

    }
// trip minh tao ra
    @Override
    public Page<TripResponse> getMyTripByStatus(int page, int size, TripStatus status) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("departureTime").descending());
        Page<Trip> tripPage = null;
        if(status != null)
        {
            tripPage = tripRepository.findByStatusAndOwner(status,getCurrentUser(), pageable);
        }
        else {
            tripPage = tripRepository.findAllByOwner( getCurrentUser(),pageable);
        }

        return tripPage.map(tripMapper::entityToResponse);
    }

    @Override
    public Page<OrderResponse> getOrdersForTrip(int page, int size) {
        User shopper = getCurrentUser();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Order> orderPage = orderRepository.findByShopOwnerAndStatus(shopper,OrderStatus.CONFIRMED, pageable);

        return orderPage.map(orderMapper::toResponse);
    }
 // get trip mà mình đã tham gia, tức là những trip mà mình đã gửi yêu cầu join và được duyệt, hoặc những trip mà mình là chủ trip
    @Override
    public PageResponse<TripResponse> getTripJoinedByCurrentUser(int page, int size, TripStatus status) {
        List<TripJoinRequest> tripResponses = new ArrayList<>();
        if(status == null){
            tripResponses = tripJoinRequestRepository.findByFarmerAndStatusOrderByCreatedAtAsc(getCurrentUser(), TripJoinStatus.ACCEPTED);
        }
        else {
            tripResponses = tripJoinRequestRepository.findByFarmerAndStatusAndTrip_StatusOrderByCreatedAtDesc(getCurrentUser(), TripJoinStatus.ACCEPTED, status);
        }
        Set<Trip> trips =  new HashSet<>();
        for(TripJoinRequest request : tripResponses)
        {
            trips.add(request.getTrip());
        }

        Long from = (long) page * size;
        Long to = Math.min(from + size, trips.size());
        List<Trip> result = new ArrayList<>();
        if(from < trips.size())
        {
            result = trips.stream().toList().subList(from.intValue(), to.intValue());
        }
        return PageResponse.<TripResponse>builder()
                .content(result.stream().map(tripMapper::entityToResponse).toList())
                .page(page)
                .size(size)
                .totalElements(trips.size())
                .totalPages(((int) Math.ceil((double) trips.size() / size)))
                .last((page + 1) * size >= trips.size())
                .build();
    }

    @Override
    public TripStopResponse updateTripStopToPicked(Integer stopId) {
        TripStop tripStop = tripStopRepository.findById(stopId).orElseThrow(
                () -> new AppException(ErrorCode.TRIP_STOP_NOT_FOUND)
        );
        tripStop.setPickup(true);
        return tripStropMapper.toTripStop(tripStopRepository.save(tripStop));
    }

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }
}
