package com.swd301.foodmarket.controller;

import com.swd301.foodmarket.dto.request.CreatePaymentRequest;
import com.swd301.foodmarket.dto.request.PayOSWebhookRequest;
import com.swd301.foodmarket.dto.response.PaymentResponse;
import com.swd301.foodmarket.service.PaymentService;
import com.swd301.foodmarket.dto.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ApiResponse<PaymentResponse> createPayment(@RequestBody @Valid CreatePaymentRequest request) {
        return ApiResponse.<PaymentResponse>builder()
                .result(paymentService.createPayment(request))
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<PaymentResponse> getById(@PathVariable String id) {
        return ApiResponse.<PaymentResponse>builder()
                .result(paymentService.getByIdOrOrderCode(id))
                .build();
    }

    @PutMapping("/{id}/status")
    public ApiResponse<PaymentResponse> updateStatus(
            @PathVariable Integer id,
            @RequestParam String status
    ) {
        return ApiResponse.<PaymentResponse>builder()
                .result(paymentService.updateStatus(id, status))
                .build();
    }

    @PostMapping("/payos")
    public ApiResponse<PaymentResponse> createPaymentPayOS(
            @Valid @RequestBody CreatePaymentRequest request
    ) {
        return ApiResponse.<PaymentResponse>builder()
                .result(paymentService.createPaymentWithPayOS(request))
                .build();
    }


    @PostMapping("/{orderCode}/confirm")
    public ApiResponse<String> confirmPayment(@PathVariable String orderCode) {

        paymentService.confirmPaymentByOrderCode(orderCode);

        return ApiResponse.<String>builder()
                .result("OK")
                .build();
    }
    @PostMapping("/{orderCode}/cancel")
    public ApiResponse<String> cancelPayment(@PathVariable String orderCode) {
        log.info("Cancelling payment for orderCode: {}", orderCode);
        paymentService.cancelPaymentByOrderCode(orderCode);

        return ApiResponse.<String>builder()
                .result("CANCELLED")
                .build();
    }

    @PostMapping("/payos/webhook")
    public ApiResponse<String> payosWebhook(@RequestBody PayOSWebhookRequest request) {
        log.info("Received PayOS webhook for orderCode: {}", request != null ? request.getData() : "null");
        // Always return OK so PayOS can auto-redirect the checkout page
        return ApiResponse.<String>builder()
                .result("OK")
                .build();
    }
}