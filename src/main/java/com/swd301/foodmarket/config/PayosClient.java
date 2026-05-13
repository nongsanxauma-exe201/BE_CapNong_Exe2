package com.swd301.foodmarket.config;

import com.swd301.foodmarket.dto.request.PayosPayoutRequest;
import com.swd301.foodmarket.dto.response.PayosPayoutResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class PayosClient {

    private final PayOSConfig payOSConfig;
    private final RestTemplate restTemplate = new RestTemplate();

    public PayosPayoutResponse payout(PayosPayoutRequest request) {

        // 👉 MOCK trước (DEV MODE)
//        if (true) {
//            return new PayosPayoutResponse(
//                    true,
//                    "PAYOS_" + System.currentTimeMillis(),
//                    "Mock payout success"
//            );
//        }

        // 👉 Khi tích hợp thật:
        /*
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-client-id", payOSConfig.getClientId());
        headers.set("x-api-key", payOSConfig.getApiKey());

        HttpEntity<PayosPayoutRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<PayosPayoutResponse> response = restTemplate.exchange(
                "https://api.payos.vn/payout",
                HttpMethod.POST,
                entity,
                PayosPayoutResponse.class
        );

        return response.getBody();
        */
        return null;
    }
}