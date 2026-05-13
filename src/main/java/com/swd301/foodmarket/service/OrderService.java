package com.swd301.foodmarket.service;

import com.swd301.foodmarket.dto.request.OrderCreationRequest;
import com.swd301.foodmarket.dto.request.OrderUpdateRequest;
import com.swd301.foodmarket.dto.response.OrderResponse;
import com.swd301.foodmarket.dto.response.PageResponse;

import java.math.BigDecimal;
import java.util.List;

public interface OrderService {
    OrderResponse createOrder(OrderCreationRequest request);
    List<OrderResponse> getAllOrders();
    PageResponse<OrderResponse> getAllOrdersPaged(int page, int size);
    OrderResponse updateOrder(Integer id, OrderUpdateRequest request);
    List<OrderResponse> getOrdersByUserId(Integer userId);
    PageResponse<OrderResponse> getOrdersByUserIdPaged(Integer userId, int page, int size);
    OrderResponse getOrderById(Integer id);
    void deleteOrder(Integer id);

    BigDecimal calculateShippingFee(String shippingAddress, Integer shopId);
}