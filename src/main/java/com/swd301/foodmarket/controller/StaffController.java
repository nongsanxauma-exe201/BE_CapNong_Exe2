package com.swd301.foodmarket.controller;

import com.swd301.foodmarket.dto.response.ApiResponse;
import com.swd301.foodmarket.dto.response.AvailableShipperResponse;
import com.swd301.foodmarket.dto.response.OrderResponse;
import com.swd301.foodmarket.service.StaffService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/staff")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class StaffController {

    StaffService staffService;

    /**
     * Lấy danh sách shipper đang online (có bản ghi ShipperLocation)
     * GET /staff/shippers/available
     */
    @GetMapping("/shippers/available")
    @PreAuthorize("hasRole('STAFF')")
    public ApiResponse<List<AvailableShipperResponse>> getAvailableShippers() {
        log.info("[STAFF] Get available shippers");
        return ApiResponse.<List<AvailableShipperResponse>>builder()
                .result(staffService.getAvailableShippers())
                .build();
    }

    /**
     * Staff assign shipper cho đơn đã đóng gói xong (status READY)
     * Sau khi assign, đơn chuyển sang CONFIRMED để shipper tự nhận qua app
     * POST /staff/orders/{orderId}/assign-shipper?shipperId=...
     */
    @PostMapping("/orders/{orderId}/assign-shipper")
    @PreAuthorize("hasRole('STAFF')")
    public ApiResponse<OrderResponse> assignShipper(
            @PathVariable Integer orderId,
            @RequestParam Integer shipperId
    ) {
        log.info("[STAFF] Assign shipper {} to order {}", shipperId, orderId);
        return ApiResponse.<OrderResponse>builder()
                .result(staffService.assignShipperToOrder(orderId, shipperId))
                .message("Shipper assigned successfully")
                .build();
    }
}
