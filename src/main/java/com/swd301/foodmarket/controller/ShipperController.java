package com.swd301.foodmarket.controller;

import com.swd301.foodmarket.dto.request.UpdateOrderStatusRequest;
import com.swd301.foodmarket.dto.response.ApiResponse;
import com.swd301.foodmarket.dto.response.AvailableOrderResponse;
import com.swd301.foodmarket.dto.response.ShipperLocationResponse;
import com.swd301.foodmarket.dto.response.ShipperOrderResponse;
import com.swd301.foodmarket.service.ShipperService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/shipper")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ShipperController {

    ShipperService shipperService;

    /**
     * Shipper xem top 6 đơn gần nhất dựa vào GPS hiện tại
     *
     * Frontend gọi: navigator.geolocation.getCurrentPosition() rồi gửi lat/lng lên
     *
     * GET /shipper/orders/nearby?lat=10.762&lng=106.660
     */
    @GetMapping("/orders/nearby")
    @PreAuthorize("hasRole('SHIPPER')")
    public ApiResponse<List<AvailableOrderResponse>> getNearbyOrders(
            @RequestParam @NotNull Double lat,
            @RequestParam @NotNull Double lng
    ) {
        log.info("[SHIPPER] Get nearby orders at ({}, {})", lat, lng);
        return ApiResponse.<List<AvailableOrderResponse>>builder()
                .result(shipperService.getAvailableOrdersNearby(lat, lng))
                .build();
    }

    /**
     * Shipper nhận đơn hàng
     * POST /shipper/orders/{orderId}/accept
     */
    @PostMapping("/orders/{orderId}/accept")
    @PreAuthorize("hasRole('SHIPPER')")
    public ApiResponse<ShipperOrderResponse> acceptOrder(@PathVariable Integer orderId) {
        log.info("[SHIPPER] Accept order: {}", orderId);
        return ApiResponse.<ShipperOrderResponse>builder()
                .result(shipperService.acceptOrder(orderId))
                .message("Order accepted successfully")
                .build();
    }

    /**
     * Shipper cập nhật trạng thái đơn (DELIVERED / FAILED)
     * POST /shipper/orders/{orderId}/status
     */
    @PostMapping("/orders/{orderId}/status")
    @PreAuthorize("hasRole('SHIPPER')")
    public ApiResponse<ShipperOrderResponse> updateOrderStatus(
            @PathVariable Integer orderId,
            @RequestBody @Valid UpdateOrderStatusRequest request
    ) {
        log.info("[SHIPPER] Update order {} → {}", orderId, request.getStatus());
        return ApiResponse.<ShipperOrderResponse>builder()
                .result(shipperService.updateOrderStatus(orderId, request))
                .build();
    }

    /**
     * Shipper xem đơn của mình
     * GET /shipper/orders/my
     */
    @GetMapping("/orders/my")
    @PreAuthorize("hasRole('SHIPPER')")
    public ApiResponse<List<ShipperOrderResponse>> getMyOrders() {
        log.info("[SHIPPER] Get my orders");
        return ApiResponse.<List<ShipperOrderResponse>>builder()
                .result(shipperService. getMyOrders())
                .build();
    }

    /**
     * Buyer/Admin lấy vị trí shipper đang giao đơn (REST fallback)
     * GET /shipper/location/order/{orderId}
     */
    @GetMapping("/location/order/{orderId}")
    @PreAuthorize("hasAnyRole('BUYER', 'ADMIN', 'SHIPPER', 'SHOP_OWNER')")
    public ApiResponse<ShipperLocationResponse> getShipperLocation(@PathVariable Integer orderId) {
        log.info("[TRACKING] Get shipper location for order: {}", orderId);
        return ApiResponse.<ShipperLocationResponse>builder()
                .result(shipperService.getShipperLocationByOrder(orderId))
                .build();
    }
}