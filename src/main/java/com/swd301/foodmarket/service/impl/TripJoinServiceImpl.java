package com.swd301.foodmarket.service.impl;

import com.swd301.foodmarket.dto.goong.LatLong;
import com.swd301.foodmarket.dto.request.CreateTripJoinRequest;
import com.swd301.foodmarket.dto.response.TripJoinRequestResponse;
import com.swd301.foodmarket.entity.*;
import com.swd301.foodmarket.enums.TripJoinStatus;
import com.swd301.foodmarket.exception.AppException;
import com.swd301.foodmarket.exception.ErrorCode;
import com.swd301.foodmarket.mapper.TripJoinRequestMapper;
import com.swd301.foodmarket.mapper.TripMapper;
import com.swd301.foodmarket.repository.OrderRepository;
import com.swd301.foodmarket.repository.TripJoinRequestRepository;
import com.swd301.foodmarket.repository.TripRepository;
import com.swd301.foodmarket.repository.UserRepository;
import com.swd301.foodmarket.service.GoongMapService;
import com.swd301.foodmarket.service.RouteOptimizationService;
import com.swd301.foodmarket.service.TripJoinService;
import com.swd301.foodmarket.service.TripService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TripJoinServiceImpl implements TripJoinService {

    private final TripJoinRequestRepository tripJoinRequestRepository;
    private final TripService tripService;
    private final RouteOptimizationService routeOptimizationService;
    private final TripJoinRequestMapper tripJoinRequestMapper;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final TripRepository tripRepository;
    private final GoongMapService goongMapService;


    @Override
    @Transactional
    public TripJoinRequestResponse requestToJoin(Integer tripId,CreateTripJoinRequest request) {
        Trip trip = tripRepository.findById(tripId).orElseThrow(
                () -> new AppException(ErrorCode.TRIP_NOT_FOUND)
        );
        List<Order> orders = new ArrayList<>();
        for(Integer id : request.getOrderIds())
        {
            orders.add( orderRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND)));
        }

        double requestedKg = calculaReqeustedKg(request);
        if (trip.getAvailableCapacityKg() < requestedKg) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        LatLong latLong = null;
        if(request.getAddress() == null && request.getPickupLat() != null && request.getPickupLng() != null) {
            latLong = new LatLong(request.getPickupLat(), request.getPickupLng());
        }
        else {
            latLong = goongMapService.geocodeAddress(request.getAddress());

        }
        double distanceKm =  goongMapService.getDistanceKm(
                latLong,
                new LatLong(trip.getWarehouse().getLatitude(), trip.getWarehouse().getLongitude())
        );;
        TripJoinRequest joinRequest = TripJoinRequest.builder()
                .trip(trip)
                .farmer(getCurrentUser())
                .requestedKg(requestedKg)
                .priceOffer(BigDecimal.valueOf(trip.getPricePerKm()*distanceKm))
                .status(TripJoinStatus.PENDING)
                .orders(orders)
                .pickupLng(latLong.getLongitude())
                .pickupLat(latLong.getLatitude())
                .build();

        return tripJoinRequestMapper.entityToResponse(tripJoinRequestRepository.save(joinRequest));
    }
    public double calculaReqeustedKg(CreateTripJoinRequest request)
    {
        double totalKg  = 0.0;
        for(Integer id : request.getOrderIds())
        {
            Order order = orderRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
            for(OrderDetail detail : order.getOrderDetails())
            {
                if(detail.getProduct().getUnit().equals("kg"))
                {
                    totalKg += detail.getQuantity() ;
                }
                 else if(detail.getProduct().getUnit().equals("g"))
                {
                    totalKg += detail.getQuantity() / 1000.0 ;
                }
                 else if(detail.getProduct().getUnit().equals("chai"))
                {
                    // giả sử 1 chai là 0.5kg
                    totalKg += detail.getQuantity() * 0.5 ;
                }
                    else if(detail.getProduct().getUnit().equals("hộp") || detail.getProduct().getUnit().equals("hũ"))
                {
                    // giả sử 1 hộp là 0.3kg
                    totalKg += detail.getQuantity() * 0.3 ;

                }
                    else if(detail.getProduct().getUnit().equals("cây"))
                {
                    // giả sử 1 cây là 0.2kg
                    totalKg += detail.getQuantity() * 0.2 ;
                }
                    else if(detail.getProduct().getUnit().equals("bịch") || detail.getProduct().getUnit().equals("túi"))
                {                    // giả sử 1 bịch là 0.4kg
                    totalKg += detail.getQuantity() * 0.4 ;
                }
                    else if(detail.getProduct().getUnit().equals("bó"))
                {
                    // giả sử 1 bó là 0.6kg
                    totalKg += detail.getQuantity() * 0.6 ;
                }

            }
        }
        return totalKg;
    }

    @Override
    @Transactional
    public TripJoinRequestResponse approveJoinRequest(Integer requestId) {
        TripJoinRequest request = tripJoinRequestRepository.findById(requestId)
                .orElseThrow(() -> new AppException(ErrorCode.TRIP_JOIN_REQUEST_NOT_FOUND));

        if (!request.getTrip().getOwner().getId().equals(getCurrentUser().getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        request.setStatus(TripJoinStatus.ACCEPTED);
        
        Trip trip = request.getTrip();
        trip.setAvailableCapacityKg(trip.getAvailableCapacityKg() - request.getRequestedKg());
        
        // Recalculate routes
        routeOptimizationService.calculateOptimalRoute(trip);
        
        return tripJoinRequestMapper.entityToResponse( tripJoinRequestRepository.save(request));
    }

    @Override
    @Transactional
    public TripJoinRequestResponse rejectJoinRequest(Integer requestId) {
        TripJoinRequest request = tripJoinRequestRepository.findById(requestId)
                .orElseThrow(() -> new AppException(ErrorCode.TRIP_JOIN_REQUEST_NOT_FOUND));

        if (!request.getTrip().getOwner().getId().equals(getCurrentUser().getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        request.setStatus(TripJoinStatus.REJECTED);
        return tripJoinRequestMapper.entityToResponse( tripJoinRequestRepository.save(request));
    }
    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }
}
