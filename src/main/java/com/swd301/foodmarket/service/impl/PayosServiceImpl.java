package com.swd301.foodmarket.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swd301.foodmarket.config.PayOSConfig;
import com.swd301.foodmarket.config.PayosClient;
import com.swd301.foodmarket.dto.request.PayOSCreateRequest;
import com.swd301.foodmarket.dto.request.PayosPayoutRequest;
import com.swd301.foodmarket.dto.response.PayOSCreateResponse;
import com.swd301.foodmarket.dto.response.PayosPayoutResponse;
import com.swd301.foodmarket.dto.response.PayOSPaymentInfoResponse;
import com.swd301.foodmarket.entity.PayosPayoutResult;
import com.swd301.foodmarket.service.PayosService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class PayosServiceImpl implements PayosService {

    private final PayOSConfig payOSConfig;
    private final PayosClient payosClient;
    @Value("${payos.client-id}")
    private String clientId;

    @Value("${payos.api-key}")
    private String apiKey;

    @Value("${payos.checksum-key}")
    private String checksumKey;

    @Value("${payos.api-url}")
    private String apiUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();
    @Value("${FRONTEND_URL}")
    private String frontUrl;

    @Override
    public PayosPayoutResult payout(
            String bankAccountNumber,
            String bankName,
            String bankAccountHolder,
            BigDecimal amount
    ) {
        String orderCode = "WD_" + java.util.UUID.randomUUID();

        PayosPayoutRequest request = PayosPayoutRequest.builder()
                .clientId(payOSConfig.getClientId())
                .bankAccountNumber(bankAccountNumber)
                .bankName(bankName)
                .accountHolder(bankAccountHolder)
                .amount(amount)
                .orderCode(orderCode)
                .checksum(buildChecksum(orderCode, amount))
                .build();

        PayosPayoutResponse response = payosClient.payout(request);

        return PayosPayoutResult.builder()
                .success(response.isSuccess())
                .orderCode(orderCode)                 // lưu DB
                .transactionId(response.getTransactionId())
                .message(response.getMessage())
                .checkoutUrl(response.getCheckoutUrl())
                .qrCodeUrl(response.getQrCodeUrl())
                .build();
    }

    private String buildChecksum(String orderCode, BigDecimal amount) {
        String raw = String.format(
                "amount=%s&orderCode=%s&clientId=%s&checksumKey=%s",
                amount.toPlainString(),
                orderCode,
                payOSConfig.getClientId(),
                payOSConfig.getChecksumKey()
        );
        return DigestUtils.sha256Hex(raw);
    }

    public PayOSCreateResponse createPaymentLink(PayOSCreateRequest request) {
        try {
            String signature = generateSignature(request);
            request.setSignature(signature);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-client-id", clientId);
            headers.set("x-api-key", apiKey);

            HttpEntity<PayOSCreateRequest> entity = new HttpEntity<>(request, headers);

            log.info("📤 PayOS REQUEST: {}", mapper.writeValueAsString(request));

            ResponseEntity<PayOSCreateResponse> response = restTemplate.exchange(
                    apiUrl, HttpMethod.POST, entity, PayOSCreateResponse.class
            );

            log.info("✅ PayOS RESPONSE STATUS: {}", response.getStatusCode());
            log.info("✅ PayOS RESPONSE BODY: {}", mapper.writeValueAsString(response.getBody()));

            return response.getBody();

        } catch (Exception e) {
            log.error("❌ Lỗi khi gọi PayOS createPaymentLink | request = {}", request, e);
            return null;
        }
    }

    // Tạo QR cho rút tiền, dùng withdrawRequestId làm orderCode để dễ tracking
    @Override
    public PayosPayoutResult createQr(
            BigDecimal amount,
            Long orderCode,
            Integer id,
            String bankName,
            String bankAccountNumber,
            String bankAccountHolder,
            String returnUrl,
            String cancelUrl
    ) {
        String description = truncateDescription("Rut tien " + bankAccountHolder);
        if ("REFUND".equals(bankName)) {
            description = truncateDescription("Hoan tien " + bankAccountHolder);
        }

        PayOSCreateRequest request = PayOSCreateRequest.builder()
                .orderCode(Long.valueOf(orderCode))
                .amount(amount.longValue())
                .description(description)
                .returnUrl(returnUrl)
                .cancelUrl(cancelUrl)
                .build();

        PayOSCreateResponse response = createPaymentLink(request);

        if (response == null || response.getData() == null) {
            return PayosPayoutResult.builder()
                    .success(false)
                    .message("Không tạo được QR PayOS")
                    .build();
        }

        return PayosPayoutResult.builder()
                .success(true)
                .orderCode(String.valueOf(orderCode))
                .transactionId(response.getData().getPaymentLinkId())
                .checkoutUrl(response.getData().getCheckoutUrl())
                .qrCodeUrl(response.getData().getQrCode())
                .build();
    }
    public PayosPayoutResult createOrderPaymentQr(
            BigDecimal amount,
            Long orderCode,
            Integer orderId,
            String customerName
    ) {

        PayOSCreateRequest request = PayOSCreateRequest.builder()
                .orderCode(orderCode)
                .amount(amount.longValue())
                .description(truncateDescription("Thanh toan " + customerName))
                .returnUrl(frontUrl+"/payment/success")
                .cancelUrl(frontUrl+"/payment/cancel")
                .build();

        PayOSCreateResponse response = createPaymentLink(request);

        if (response == null || response.getData() == null) {
            return PayosPayoutResult.builder()
                    .success(false)
                    .message("Không tạo được QR PayOS")
                    .build();
        }

        return PayosPayoutResult.builder()
                .success(true)
                .orderCode(String.valueOf(orderCode))
                .transactionId(response.getData().getPaymentLinkId())
                .checkoutUrl(response.getData().getCheckoutUrl())
                .qrCodeUrl(response.getData().getQrCode())
                .build();
    }
    private String truncateDescription(String desc) {
        if (desc == null) return "Thanh toan FoodMarket";
        // Remove accents
        String normalized = java.text.Normalizer.normalize(desc, java.text.Normalizer.Form.NFD);
        String nonAccented = normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        // Only keep alphanumeric and space
        String sanitized = nonAccented.replaceAll("[^a-zA-Z0-9 ]", "");
        if (sanitized.length() > 25) {
            return sanitized.substring(0, 25);
        }
        return sanitized;
    }

    private String generateSignature(PayOSCreateRequest request) throws Exception {
        SortedMap<String, String> sortedParams = new TreeMap<>();
        sortedParams.put("amount", String.valueOf(request.getAmount()));
        sortedParams.put("cancelUrl", request.getCancelUrl());
        sortedParams.put("description", request.getDescription());
        sortedParams.put("orderCode", String.valueOf(request.getOrderCode()));
        sortedParams.put("returnUrl", request.getReturnUrl());

        StringBuilder rawData = new StringBuilder();
        for (Map.Entry<String, String> entry : sortedParams.entrySet()) {
            if (rawData.length() > 0) rawData.append("&");
            rawData.append(entry.getKey()).append("=").append(entry.getValue());
        }

        Mac hmac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(checksumKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        hmac.init(secretKey);
        byte[] bytes = hmac.doFinal(rawData.toString().getBytes(StandardCharsets.UTF_8));

        StringBuilder hash = new StringBuilder();
        for (byte b : bytes) {
            hash.append(String.format("%02x", b));
        }

        return hash.toString();
    }

    @Override
    public PayOSPaymentInfoResponse getPaymentLinkInformation(Long orderCode) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-client-id", clientId);
            headers.set("x-api-key", apiKey);

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            // Standard PayOS status check URL
            String checkUrl = "https://api.payos.vn/v2/payment-links/" + orderCode;

            ResponseEntity<PayOSPaymentInfoResponse> response = restTemplate.exchange(
                    checkUrl, HttpMethod.GET, entity, PayOSPaymentInfoResponse.class
            );

            log.info("🔍 Check PayOS Status for {}: {}", orderCode, response.getStatusCode());
            return response.getBody();

        } catch (Exception e) {
            log.error("❌ Lỗi khi lấy thông tin thanh toán PayOS | orderCode = {}", orderCode, e);
            return null;
        }
    }

}