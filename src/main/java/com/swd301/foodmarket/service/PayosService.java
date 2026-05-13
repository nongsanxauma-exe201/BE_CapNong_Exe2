package com.swd301.foodmarket.service;

import com.swd301.foodmarket.dto.request.PayOSCreateRequest;
import com.swd301.foodmarket.dto.response.PayOSCreateResponse;
import com.swd301.foodmarket.dto.response.PayOSPaymentInfoResponse;
import com.swd301.foodmarket.entity.PayosPayoutResult;

import java.math.BigDecimal;

public interface PayosService {
        PayosPayoutResult payout(
            String bankAccountNumber,
            String bankName,
            String bankAccountHolder,
            BigDecimal amount
    );
    PayOSCreateResponse createPaymentLink(PayOSCreateRequest request);

    PayosPayoutResult createQr(
            BigDecimal amount,
            Long orderCode,
            Integer id,
            String bankName,
            String bankAccountNumber,
            String bankAccountHolder,
            String returnUrl,
            String cancelUrl
    );
    PayosPayoutResult createOrderPaymentQr(
            BigDecimal amount,
            Long orderCode,
            Integer orderId,
            String customerName
    );

    PayOSPaymentInfoResponse getPaymentLinkInformation(Long orderCode);
}
