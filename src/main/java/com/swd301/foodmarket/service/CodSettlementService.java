package com.swd301.foodmarket.service;

import com.swd301.foodmarket.dto.response.OrderResponse;

import java.util.List;

public interface CodSettlementService {
    void settleCodOrder(Integer orderId);

    List<OrderResponse> getPendingCodSettlementOrders();
}