package com.swd301.foodmarket.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
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
import com.fasterxml.jackson.databind.DeserializationFeature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
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

    private final ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private final RestTemplate restTemplate = createPayosRestTemplate();
    private final HttpClient payosHttpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    @Value("${FRONTEND_URL}")
    private String frontUrl;

    private RestTemplate createPayosRestTemplate() {
        // PayOS GET có thể chậm; cần timeout đủ dài + retry ở getPaymentLinkInformation
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(15_000);
        factory.setReadTimeout(35_000);
        RestTemplate template = new RestTemplate(factory);
        MappingJackson2HttpMessageConverter jackson = new MappingJackson2HttpMessageConverter(mapper);
        template.getMessageConverters().removeIf(MappingJackson2HttpMessageConverter.class::isInstance);
        template.getMessageConverters().add(0, jackson);
        return template;
    }

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

    @Override
    public boolean verifyWebhookSignature(JsonNode webhookBody) {
        if (webhookBody == null || !webhookBody.hasNonNull("signature") || !webhookBody.has("data")) {
            return false;
        }
        JsonNode data = webhookBody.get("data");
        if (data == null || data.isNull()) {
            return false;
        }
        try {
            String expectedSignature = webhookBody.get("signature").asText();
            String computed = hmacSha256(buildWebhookDataString(data));
            return expectedSignature.equalsIgnoreCase(computed);
        } catch (Exception e) {
            log.error("Failed to verify PayOS webhook signature", e);
            return false;
        }
    }

    private String buildWebhookDataString(JsonNode data) {
        TreeMap<String, String> sortedParams = new TreeMap<>();
        data.fields().forEachRemaining(entry -> {
            JsonNode value = entry.getValue();
            if (value == null || value.isNull()) {
                sortedParams.put(entry.getKey(), "");
            } else if (value.isNumber()) {
                sortedParams.put(entry.getKey(), value.asText());
            } else {
                sortedParams.put(entry.getKey(), value.asText());
            }
        });

        StringBuilder rawData = new StringBuilder();
        for (Map.Entry<String, String> entry : sortedParams.entrySet()) {
            if (rawData.length() > 0) {
                rawData.append("&");
            }
            rawData.append(entry.getKey()).append("=").append(entry.getValue());
        }
        return rawData.toString();
    }

    private String hmacSha256(String rawData) throws Exception {
        Mac hmac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(checksumKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        hmac.init(secretKey);
        byte[] bytes = hmac.doFinal(rawData.getBytes(StandardCharsets.UTF_8));

        StringBuilder hash = new StringBuilder();
        for (byte b : bytes) {
            hash.append(String.format("%02x", b));
        }
        return hash.toString();
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
        return getPaymentLinkInformation(String.valueOf(orderCode));
    }

    @Override
    public PayOSPaymentInfoResponse getPaymentLinkInformation(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        String base = apiUrl.endsWith("/") ? apiUrl.substring(0, apiUrl.length() - 1) : apiUrl;
        String checkUrl = base + "/" + id.trim();

        int maxAttempts = 3;
        Exception lastError = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                log.info("🔍 Check PayOS payment link (attempt {}/{}): {}", attempt, maxAttempts, checkUrl);
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(checkUrl))
                        .timeout(Duration.ofSeconds(35))
                        .header("Accept", "application/json")
                        .header("x-client-id", clientId)
                        .header("x-api-key", apiKey)
                        .GET()
                        .build();

                HttpResponse<String> response = payosHttpClient.send(request, HttpResponse.BodyHandlers.ofString());
                String body = response.body();
                log.info("🔍 Check PayOS Status for {}: http={} body={}", id, response.statusCode(), body);
                if (body == null || body.isBlank()) {
                    lastError = new IllegalStateException("Empty PayOS response body");
                    continue;
                }
                return mapper.readValue(body, PayOSPaymentInfoResponse.class);
            } catch (Exception e) {
                lastError = e;
                log.warn("❌ PayOS getPaymentLink attempt {}/{} failed | id={} | {}",
                        attempt, maxAttempts, id, e.toString());
                if (attempt < maxAttempts) {
                    try {
                        Thread.sleep(1000L * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        log.error("❌ Lỗi khi lấy thông tin thanh toán PayOS | id = {}", id, lastError);
        return null;
    }

}