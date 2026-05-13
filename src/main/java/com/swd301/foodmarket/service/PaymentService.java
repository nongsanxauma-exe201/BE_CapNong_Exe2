package com.swd301.foodmarket.service;

import com.swd301.foodmarket.dto.request.CreatePaymentRequest;
import com.swd301.foodmarket.dto.request.PayOSWebhookRequest;
import com.swd301.foodmarket.dto.response.PaymentResponse;

public interface PaymentService {

    PaymentResponse createPayment(CreatePaymentRequest request);

    PaymentResponse getById(Integer id);

    PaymentResponse getByIdOrOrderCode(String idOrOrderCode);

    PaymentResponse updateStatus(Integer id, String status);

    PaymentResponse createPaymentWithPayOS(CreatePaymentRequest request);

    void confirmPaymentByOrderCode(String orderCode);

    void cancelPaymentByOrderCode(String orderCode);

}