package com.swd301.foodmarket.service;

import com.swd301.foodmarket.dto.request.CreatePaymentRequest;
import com.swd301.foodmarket.dto.response.PaymentResponse;
import com.swd301.foodmarket.dto.response.ResumePaymentOptionsResponse;
import com.swd301.foodmarket.entity.Order;

import java.math.BigDecimal;

public interface PaymentService {

    PaymentResponse createPayment(CreatePaymentRequest request);

    PaymentResponse getById(Integer id);

    PaymentResponse getByIdOrOrderCode(String idOrOrderCode);

    PaymentResponse updateStatus(Integer id, String status);

    PaymentResponse createPaymentWithPayOS(CreatePaymentRequest request);

    void confirmPaymentByOrderCode(String orderCode);

    void confirmPaymentByOrderCode(String orderCode, Long webhookAmount);

    void cancelPaymentByOrderCode(String orderCode);

    void notifyPendingPayment(String orderCode);

    PaymentResponse getLatestPendingByOrderId(Integer orderId);

    ResumePaymentOptionsResponse getResumePaymentOptions(Integer orderId);

    PaymentResponse createBatchPayOS(Integer batchId);

    void refundPayosOrderOnCancel(Order order);

    void refundPayosOrderOnFailed(Order order);

    /** @return số tiền thực hoàn (0 nếu đã hết hạn mức hoàn) */
    BigDecimal refundQcDamage(Order order, BigDecimal amount, String reason);

}