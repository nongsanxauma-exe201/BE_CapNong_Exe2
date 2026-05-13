package com.swd301.foodmarket.controller;

import com.swd301.foodmarket.dto.request.WalletCreationRequest;
import com.swd301.foodmarket.dto.request.WithdrawRequestCreationRequest;
import com.swd301.foodmarket.dto.response.WalletResponse;
import com.swd301.foodmarket.dto.response.ApiResponse;
import com.swd301.foodmarket.dto.response.WithdrawRequestResponse;
import com.swd301.foodmarket.service.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/wallets")
@RequiredArgsConstructor
public class WalletController {

        private final WalletService walletService;

        // Shop owner xem ví
        @GetMapping("/me")
        @PreAuthorize("hasRole('SHOP_OWNER')")
        public ApiResponse<WalletResponse> myWallet() {
                return ApiResponse.<WalletResponse>builder()
                                .result(walletService.getMyWallet())
                                .build();
        }

        // Shipper xem ví
        @GetMapping("/shipper/me")
        @PreAuthorize("hasRole('SHIPPER')")
        public ApiResponse<WalletResponse> myShipperWallet() {
                return ApiResponse.<WalletResponse>builder()
                                .result(walletService.getMyShipperWallet())
                                .build();
        }

        // Shipper gửi yêu cầu rút tiền
        @PostMapping("/shipper/withdraw-requests")
        @PreAuthorize("hasRole('SHIPPER')")
        public ApiResponse<WithdrawRequestResponse> shipperWithdraw(
                        @RequestBody @Valid WithdrawRequestCreationRequest request) {
                return ApiResponse.<WithdrawRequestResponse>builder()
                                .result(walletService.createShipperWithdrawRequest(request))
                                .build();
        }

        // Shipper xem lịch sử rút tiền
        @GetMapping("/shipper/withdraw-requests/my")
        @PreAuthorize("hasRole('SHIPPER')")
        public ApiResponse<List<WithdrawRequestResponse>> myShipperWithdrawRequests() {
                return ApiResponse.<List<WithdrawRequestResponse>>builder()
                                .result(walletService.getMyShipperWithdrawRequests())
                                .build();
        }

        // Shop owner gửi yêu cầu rút tiền
        @PostMapping("/withdraw-requests")
        @PreAuthorize("hasRole('SHOP_OWNER')")
        public ApiResponse<WithdrawRequestResponse> withdraw(
                        @RequestBody @Valid WithdrawRequestCreationRequest request) {
                return ApiResponse.<WithdrawRequestResponse>builder()
                                .result(walletService.createWithdrawRequest(request))
                                .build();
        }

        // Admin xem danh sách pending
        @GetMapping("/withdraw-requests/pending")
        @PreAuthorize("hasRole('ADMIN')")
        public ApiResponse<List<WithdrawRequestResponse>> getPending() {
                return ApiResponse.<List<WithdrawRequestResponse>>builder()
                                .result(walletService.getPendingWithdrawRequests())
                                .build();
        }

        // Admin duyệt
        @PutMapping("/withdraw-requests/{id}/confirm-success")
        @PreAuthorize("hasRole('ADMIN')")
        public ApiResponse<WithdrawRequestResponse> confirmSuccess(
                        @PathVariable Integer id,
                        @RequestParam(required = false) String note) {
                return ApiResponse.<WithdrawRequestResponse>builder()
                                .result(walletService.confirmWithdrawSuccess(id, note))
                                .build();
        }

        @PostMapping("/withdraw-requests/{id}/create-qr")
        @PreAuthorize("hasRole('ADMIN')")
        public ApiResponse<WithdrawRequestResponse> createQr(@PathVariable Integer id) {
                return ApiResponse.<WithdrawRequestResponse>builder()
                                .result(walletService.createWithdrawQr(id))
                                .build();
        }

        // Admin từ chối
        @PutMapping("/withdraw-requests/{id}/reject")
        @PreAuthorize("hasRole('ADMIN')")
        public ApiResponse<WithdrawRequestResponse> reject(
                        @PathVariable Integer id,
                        @RequestParam(required = false) String note) {
                return ApiResponse.<WithdrawRequestResponse>builder()
                                .result(walletService.rejectWithdraw(id, note))
                                .build();
        }

        @GetMapping("/withdraw-requests/my")
        @PreAuthorize("hasRole('SHOP_OWNER')")
        public ApiResponse<List<WithdrawRequestResponse>> myWithdrawRequests() {
                return ApiResponse.<List<WithdrawRequestResponse>>builder()
                                .result(walletService.getMyWithdrawRequests())
                                .build();
        }

        @GetMapping("/withdraw-requests")
        @PreAuthorize("hasRole('ADMIN')")
        public ApiResponse<List<WithdrawRequestResponse>> getAllWithdrawRequests() {
                return ApiResponse.<List<WithdrawRequestResponse>>builder()
                                .result(walletService.getAllWithdrawRequests())
                                .build();
        }
}