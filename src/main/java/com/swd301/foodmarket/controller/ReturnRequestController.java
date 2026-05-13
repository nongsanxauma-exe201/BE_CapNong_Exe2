package com.swd301.foodmarket.controller;

import com.swd301.foodmarket.dto.request.ReturnActionRequest;
import com.swd301.foodmarket.dto.request.ReturnCreateRequest;
import com.swd301.foodmarket.dto.response.ApiResponse;
import com.swd301.foodmarket.dto.response.ReturnActionResponse;
import com.swd301.foodmarket.entity.ReturnRequest;
import com.swd301.foodmarket.service.ReturnRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/returns")
@RequiredArgsConstructor
public class ReturnRequestController {

    private final ReturnRequestService returnRequestService;

    @PostMapping(consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ReturnRequest> create(
            @RequestPart("data") ReturnCreateRequest request,
            @RequestPart(value = "evidence", required = false) org.springframework.web.multipart.MultipartFile evidence) {
        return ApiResponse.<ReturnRequest>builder()
                .result(returnRequestService.createReturnRequest(request, evidence))
                .build();
    }

    @GetMapping("/me")
    public ApiResponse<List<ReturnRequest>> getMyReturns() {
        return ApiResponse.<List<ReturnRequest>>builder()
                .result(returnRequestService.getMyReturns())
                .build();
    }

    @GetMapping("/shop")
    public ApiResponse<List<ReturnRequest>> getShopReturns() {
        return ApiResponse.<List<ReturnRequest>>builder()
                .result(returnRequestService.getShopReturns())
                .build();
    }

    @GetMapping("/disputes")
    public ApiResponse<List<ReturnRequest>> getDisputes() {
        return ApiResponse.<List<ReturnRequest>>builder()
                .result(returnRequestService.getDisputes())
                .build();
    }

    @PutMapping("/{id}/shop-action")
    public ApiResponse<ReturnActionResponse> shopAction(@PathVariable Integer id, @RequestBody ReturnActionRequest request) {
        return ApiResponse.<ReturnActionResponse>builder()
                .result(returnRequestService.shopAction(id, request))
                .build();
    }

    @PutMapping("/{id}/dispute")
    public ApiResponse<ReturnRequest> dispute(@PathVariable Integer id, @RequestParam String reason) {
        return ApiResponse.<ReturnRequest>builder()
                .result(returnRequestService.dispute(id, reason))
                .build();
    }

    @PutMapping("/{id}/admin-action")
    public ApiResponse<ReturnActionResponse> adminAction(@PathVariable Integer id, @RequestBody ReturnActionRequest request) {
        return ApiResponse.<ReturnActionResponse>builder()
                .result(returnRequestService.adminAction(id, request))
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<ReturnRequest> getById(@PathVariable Integer id) {
        return ApiResponse.<ReturnRequest>builder()
                .result(returnRequestService.getById(id))
                .build();
    }

    @GetMapping("/{id}/check-payout")
    public ApiResponse<ReturnActionResponse> checkPayout(@PathVariable Integer id) {
        return ApiResponse.<ReturnActionResponse>builder()
                .result(returnRequestService.checkPayoutStatus(id))
                .build();
    }

    @GetMapping("/auto-check-payout")
    public ApiResponse<ReturnActionResponse> autoCheckPayout(@RequestParam Long orderCode) {
        return ApiResponse.<ReturnActionResponse>builder()
                .result(returnRequestService.autoCheckByOrderCode(orderCode))
                .build();
    }
}
