package com.swd301.foodmarket.controller;

import com.swd301.foodmarket.dto.response.ApiResponse;
import com.swd301.foodmarket.dto.response.OrderResponse;
import com.swd301.foodmarket.service.CodSettlementService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/cod")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class CodSettlementController {

    private final CodSettlementService codSettlementService;

    @GetMapping("/pending")
    public ApiResponse<List<OrderResponse>> getPendingCodSettlementOrders() {
        return ApiResponse.<List<OrderResponse>>builder()
                .result(codSettlementService.getPendingCodSettlementOrders())
                .build();
    }

    @PutMapping("/orders/{orderId}/settle")
    public ApiResponse<Void> settleCodOrder(@PathVariable Integer orderId) {
        codSettlementService.settleCodOrder(orderId);

        return ApiResponse.<Void>builder()
                .message("COD_SETTLED_SUCCESS")
                .build();
    }
}