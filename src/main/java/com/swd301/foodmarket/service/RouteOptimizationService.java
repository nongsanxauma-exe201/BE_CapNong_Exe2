package com.swd301.foodmarket.service;

import com.swd301.foodmarket.entity.Trip;

public interface RouteOptimizationService {
    void calculateOptimalRoute(Trip trip);
}
