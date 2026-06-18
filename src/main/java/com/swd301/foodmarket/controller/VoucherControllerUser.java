package com.swd301.foodmarket.controller;

import com.swd301.foodmarket.dto.request.GetVoucherForOrder;
import com.swd301.foodmarket.dto.response.ApiResponse;
import com.swd301.foodmarket.dto.response.VoucherUserResponse;
import com.swd301.foodmarket.entity.VoucherUser;
import com.swd301.foodmarket.service.VoucherUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/vouchers-users")
public class VoucherControllerUser {
  private final VoucherUserService voucherUserService;

  @GetMapping
    public ApiResponse<Page<VoucherUserResponse>> getMyVouchers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
  ) {
        return ApiResponse.<Page<VoucherUserResponse>>builder()
                .result(voucherUserService.getMyVouchers(page, size))
                .build();
    }
    @PostMapping("/{voucherCode}/recieve")
    public ApiResponse<VoucherUserResponse> receiveVoucher(@PathVariable String voucherCode) {
        return ApiResponse.<VoucherUserResponse>builder()
                .result(voucherUserService.createVoucherUser(voucherCode))
                .build();
    }
    @GetMapping("/{voucherUserId}")
    public ApiResponse<VoucherUserResponse> getVoucherUserById(@PathVariable UUID voucherUserId
) {
        return ApiResponse.<VoucherUserResponse>builder()
                .result(voucherUserService.getVoucherUserById(voucherUserId))
                .build();
    }

    @GetMapping("/can-receive/{voucherCode}")
    public ApiResponse<Boolean> canReceiveVoucher(@PathVariable String voucherCode) {
        return ApiResponse.<Boolean>builder()
                .result(voucherUserService.canRecieveVoucher(voucherCode))
                .build();
    }
    @PostMapping("/voucher-order")
    public ApiResponse<Page<VoucherUserResponse>> getMyVoucherOrders(
            @RequestBody GetVoucherForOrder request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
            )
    {
        return ApiResponse.<Page<VoucherUserResponse>>builder()
                .result(voucherUserService.getVoucherForOrder(request,page,size))
                .build();
    }

}
