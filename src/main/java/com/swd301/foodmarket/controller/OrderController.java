package com.swd301.foodmarket.controller;

import com.swd301.foodmarket.dto.request.CheckoutBatchRequest;
import com.swd301.foodmarket.dto.request.OrderCreationRequest;
import com.swd301.foodmarket.dto.request.OrderUpdateRequest;
import com.swd301.foodmarket.dto.request.QcBuyerDecisionRequest;
import com.swd301.foodmarket.dto.response.*;
import com.swd301.foodmarket.enums.OrderStatus;
import com.swd301.foodmarket.service.OrderService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) List<OrderStatus> statuses
    ) {
        return ApiResponse.<PageResponse<OrderResponse>>builder()
                .result(orderService.getOrdersByUserIdPaged(userId, page, size, statuses))
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
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) List<OrderStatus> statuses
    ) {
        return ApiResponse.<PageResponse<OrderResponse>>builder()
                .result(orderService.getAllOrdersPaged(page, size, statuses))
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
     * GET /orders/shipping-fee?shippingAddress=...&shopId=...&placeId=...  ← khi chọn từ suggestion
     */
    @GetMapping("/shipping-fee")
    public ApiResponse<BigDecimal> getShippingFee(
            @RequestParam String shippingAddress,
            @RequestParam Integer shopId,
            @RequestParam(required = false) String placeId
    ) {
        BigDecimal fee;
        if (placeId != null && !placeId.isBlank()) {
            log.info("[ShippingFee] placeId mode: placeId='{}', shopId={}", placeId, shopId);
            fee = orderService.calculateShippingFeeByPlaceId(placeId, shopId);
        } else {
            log.info("[ShippingFee] Preview: address='{}', shopId={}", shippingAddress, shopId);
            fee = orderService.calculateShippingFee(shippingAddress, shopId);
        }
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

    @PostMapping("/checkout-batch")
    public ApiResponse<CheckoutBatchResponse> checkoutBatch(@RequestBody @Valid CheckoutBatchRequest request) {
        return ApiResponse.<CheckoutBatchResponse>builder()
                .result(orderService.checkoutBatch(request))
                .build();
    }

    @PatchMapping(value =  "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<OrderResponse> updateOrder(
            @PathVariable Integer id,
            @RequestPart("request") @Valid OrderUpdateRequest request,
            @RequestPart(required = false,value = "files") List<MultipartFile> files
            ) {
        log.info("Request status: {}", request.getStatus());
        return ApiResponse.<OrderResponse>builder()
                .result(orderService.updateOrder(id, request,files))
                .build();
    }

    @PostMapping("/{id}/qc-buyer-decision")
    public ApiResponse<OrderResponse> qcBuyerDecision(
            @PathVariable Integer id,
            @RequestBody @Valid QcBuyerDecisionRequest request
    ) {
        return ApiResponse.<OrderResponse>builder()
                .result(orderService.processQcBuyerDecision(id, Boolean.TRUE.equals(request.getAcceptRemaining())))
                .build();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<String> deleteOrder(@PathVariable Integer id) {
        orderService.deleteOrder(id);
        return ApiResponse.<String>builder()
                .result("Delete order successfully")
                .build();
    }
    // STAFF api
    @GetMapping("/staff")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ApiResponse<Page<OrderResponse>> getOrdersForStaff(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) OrderStatus status
    ) {
        return ApiResponse.<Page<OrderResponse>>builder()
                .result(orderService.getOrdersForStaff(page, size,status))
                .build();
    }
    @GetMapping("/staff/report")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ApiResponse<StaffOrderReportResponse> getReportForStaff(
    ) {
        return ApiResponse.<StaffOrderReportResponse>builder()
                .result(orderService.generateStaffOrderReport())
                .build();
    }
    @GetMapping("/shop/reports")
    public ApiResponse<OrderShopReport> getReportForShopDashBoard(
            @RequestParam String type
    ) {
        return ApiResponse.<OrderShopReport>builder()
                .result(orderService.orderShopReport(type))
                .build();
    }
    @GetMapping("/shop/statistic")
    public ApiResponse<OrderResponseReport> getReportForShop(
    ) {
        return ApiResponse.<OrderResponseReport>builder()
                .result(orderService.orderReport())
                .build();
    }


}