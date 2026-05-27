package com.swd301.foodmarket.service;

import com.swd301.foodmarket.dto.response.AvailableShipperResponse;
import com.swd301.foodmarket.dto.response.OrderResponse;

import java.util.List;

public interface StaffService {
    List<AvailableShipperResponse> getAvailableShippers();
    OrderResponse assignShipperToOrder(Integer orderId, Integer shipperId);
}
