package com.swd301.foodmarket.service;

import com.swd301.foodmarket.dto.response.RouteData;

public interface RoutingService {

    RouteData getRoute(
            Double startLat,
            Double startLng,
            Double endLat,
            Double endLng
    );
}
