package com.swd301.foodmarket.controller;

import com.swd301.foodmarket.dto.request.VoucherCreateRequest;
import com.swd301.foodmarket.dto.request.VoucherUpdateRequest;
import com.swd301.foodmarket.dto.response.ApiResponse;
import com.swd301.foodmarket.dto.response.VoucherResponse;
import com.swd301.foodmarket.service.VoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/vouchers")
@RequiredArgsConstructor
public class VoucherController {

    private final VoucherService voucherService;

    @GetMapping("/shop")
    public ApiResponse<Page<VoucherResponse>> getShopVouchers(
            @RequestParam (defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "true") boolean active

    ) {

        return ApiResponse.<Page<VoucherResponse>>builder()
                .result(voucherService.getMyVouchers(page, size, active))
                .build(); // Replace with actual implementation
    }
    @PostMapping
    public ApiResponse<VoucherResponse> createVoucher(@RequestBody VoucherCreateRequest voucherRequest) {
        return ApiResponse.<VoucherResponse>builder()
                .result(voucherService.createVoucher(voucherRequest))
                .build();
    }

    @GetMapping("/admin/platform")
    public ApiResponse<Page<VoucherResponse>> getPlatformVouchers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "true") boolean active
    ) {
        return ApiResponse.<Page<VoucherResponse>>builder()
                .result(voucherService.getPlatformVouchers(page, size, active))
                .build();
    }

    @PostMapping("/admin/platform")
    public ApiResponse<VoucherResponse> createPlatformVoucher(@RequestBody VoucherCreateRequest voucherRequest) {
        return ApiResponse.<VoucherResponse>builder()
                .result(voucherService.createPlatformVoucher(voucherRequest))
                .build();
    }
    @PutMapping("/{voucherCode}")
    public ApiResponse<VoucherResponse> updateVoucher(@PathVariable String voucherCode, @RequestBody VoucherUpdateRequest voucherRequest) {
        return ApiResponse.<VoucherResponse>builder()
                .result(voucherService.updateVoucher(voucherCode, voucherRequest))
                .build();
    }

    @DeleteMapping("/{voucherCode}")
    public ApiResponse<Void> deleteVoucher(@PathVariable String voucherCode) {
        voucherService.deleteVoucher(voucherCode);
        return ApiResponse.<Void>builder()
                .message("Voucher deleted successfully")
                .build();
    }
    @PutMapping("/{voucherCode}/activate")
    public ApiResponse<VoucherResponse> activateVoucher(@PathVariable String voucherCode) {
        return ApiResponse.<VoucherResponse>builder()
                .result(voucherService.activateVoucher(voucherCode))
                .build();
    }
    @GetMapping("/{voucherCode}")
    public ApiResponse<VoucherResponse> getVoucherByCode(@PathVariable String voucherCode) {
        return ApiResponse.<VoucherResponse>builder()
                .result(voucherService.getVoucherByCode(voucherCode))
                .build();
    }
    @GetMapping("/{shopId}/buyer")
    public ApiResponse<Page<VoucherResponse>> getShopBuyer(
            @RequestParam (defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @PathVariable Integer shopId

    ) {

        return ApiResponse.<Page<VoucherResponse>>builder()
                .result(voucherService.getVoucherForBuyer(page, size,shopId))
                .build();
    }


}
