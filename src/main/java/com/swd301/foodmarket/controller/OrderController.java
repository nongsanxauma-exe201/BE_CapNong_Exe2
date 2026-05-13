package com.swd301.foodmarket.controller;

import com.swd301.foodmarket.dto.request.OrderCreationRequest;
import com.swd301.foodmarket.dto.request.OrderUpdateRequest;
import com.swd301.foodmarket.dto.response.ApiResponse;
import com.swd301.foodmarket.dto.response.OrderResponse;
import com.swd301.foodmarket.dto.response.PageResponse;
import com.swd301.foodmarket.service.OrderService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class OrderController {
    private OrderService orderService;

    @GetMapping("/user/{userId}")
    public ApiResponse<List<OrderResponse>> getOrdersByUserId(@PathVariable Integer userId) {
        return ApiResponse.<List<OrderResponse>>builder()
                .result(orderService.getOrdersByUserId(userId))
                .build();
    }

    @GetMapping("/user/{userId}/paged")
    public ApiResponse<PageResponse<OrderResponse>> getOrdersByUserIdPaged(
            @PathVariable Integer userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.<PageResponse<OrderResponse>>builder()
                .result(orderService.getOrdersByUserIdPaged(userId, page, size))
                .build();
    }

    @GetMapping
    public ApiResponse<List<OrderResponse>> getAllOrders() {
        return ApiResponse.<List<OrderResponse>>builder()
                .result(orderService.getAllOrders())
                .build();
    }

    @GetMapping("/paged")
    public ApiResponse<PageResponse<OrderResponse>> getAllOrdersPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.<PageResponse<OrderResponse>>builder()
                .result(orderService.getAllOrdersPaged(page, size))
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<OrderResponse> getOrderById(@PathVariable Integer id) {
        return ApiResponse.<OrderResponse>builder()
                .result(orderService.getOrderById(id))
                .build();
    }

    /**
     * ✅ Preview phí ship trước khi đặt hàng
     * FE gọi khi buyer nhập địa chỉ ở trang Checkout
     *
     * GET /orders/shipping-fee?shippingAddress=...&shopId=...
     */
    @GetMapping("/shipping-fee")
    public ApiResponse<BigDecimal> getShippingFee(
            @RequestParam String shippingAddress,
            @RequestParam Integer shopId
    ) {
        log.info("[ShippingFee] Preview: address='{}', shopId={}", shippingAddress, shopId);
        BigDecimal fee = orderService.calculateShippingFee(shippingAddress, shopId);
        return ApiResponse.<BigDecimal>builder()
                .result(fee)
                .build();
    }

    @PostMapping
    public ApiResponse<OrderResponse> createOrder(@RequestBody @Valid OrderCreationRequest request) {
        return ApiResponse.<OrderResponse>builder()
                .result(orderService.createOrder(request))
                .build();
    }

    @PatchMapping("/{id}")
    public ApiResponse<OrderResponse> updateOrder(
            @PathVariable Integer id,
            @RequestBody @Valid OrderUpdateRequest request
    ) {
        log.info("Request status: {}", request.getStatus());
        return ApiResponse.<OrderResponse>builder()
                .result(orderService.updateOrder(id, request))
                .build();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<String> deleteOrder(@PathVariable Integer id) {
        orderService.deleteOrder(id);
        return ApiResponse.<String>builder()
                .result("Delete order successfully")
                .build();
    }
}