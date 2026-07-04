package com.swd301.foodmarket.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.swd301.foodmarket.dto.request.CreatePaymentRequest;
import com.swd301.foodmarket.dto.response.PaymentResponse;
import com.swd301.foodmarket.dto.response.ResumePaymentOptionsResponse;
import com.swd301.foodmarket.service.PaymentService;
import com.swd301.foodmarket.service.PayosService;
import com.swd301.foodmarket.dto.response.ApiResponse;
import com.swd301.foodmarket.exception.AppException;
import com.swd301.foodmarket.exception.ErrorCode;
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
    private final PayosService payosService;

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

    @PostMapping("/{orderCode}/notify-pending")
    public ApiResponse<String> notifyPendingPayment(@PathVariable String orderCode) {
        paymentService.notifyPendingPayment(orderCode);
        return ApiResponse.<String>builder()
                .result("NOTIFIED")
                .build();
    }

    @GetMapping("/order/{orderId}/latest-pending")
    public ApiResponse<PaymentResponse> getLatestPendingByOrderId(@PathVariable Integer orderId) {
        return ApiResponse.<PaymentResponse>builder()
                .result(paymentService.getLatestPendingByOrderId(orderId))
                .build();
    }

    @GetMapping("/order/{orderId}/resume-options")
    public ApiResponse<ResumePaymentOptionsResponse> getResumePaymentOptions(@PathVariable Integer orderId) {
        return ApiResponse.<ResumePaymentOptionsResponse>builder()
                .result(paymentService.getResumePaymentOptions(orderId))
                .build();
    }

    @PostMapping("/batch/{batchId}/payos")
    public ApiResponse<PaymentResponse> createBatchPayOS(@PathVariable Integer batchId) {
        return ApiResponse.<PaymentResponse>builder()
                .result(paymentService.createBatchPayOS(batchId))
                .build();
    }

    @PostMapping("/payos/webhook")
    public ApiResponse<String> payosWebhook(@RequestBody JsonNode request) {
        log.info("Received PayOS webhook");
        if (!payosService.verifyWebhookSignature(request)) {
            log.warn("Invalid PayOS webhook signature");
            throw new AppException(ErrorCode.PAYOS_WEBHOOK_INVALID);
        }

        JsonNode data = request.get("data");
        if (data == null || data.isNull() || !data.hasNonNull("orderCode")) {
            return ApiResponse.<String>builder().result("OK").build();
        }

        String code = request.has("code") ? request.get("code").asText() : null;
        String status = data.has("status") ? data.get("status").asText() : null;
        if ("00".equals(code) || "PAID".equalsIgnoreCase(status)) {
            try {
                String orderCode = data.get("orderCode").asText();
                Long amount = data.has("amount") ? data.get("amount").asLong() : null;
                paymentService.confirmPaymentByOrderCode(orderCode, amount);
            } catch (Exception ex) {
                log.error("Failed to confirm PayOS webhook orderCode={}: {}",
                        data.get("orderCode").asText(),
                        ex.getMessage(),
                        ex);
            }
        }
        return ApiResponse.<String>builder()
                .result("OK")
                .build();
    }
}