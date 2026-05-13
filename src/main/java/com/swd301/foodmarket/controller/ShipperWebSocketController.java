package com.swd301.foodmarket.controller;

import com.swd301.foodmarket.dto.request.ShipperLocationRequest;
import com.swd301.foodmarket.dto.response.ShipperLocationResponse;
import com.swd301.foodmarket.service.ShipperService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ShipperWebSocketController {

    private final ShipperService shipperService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/shipper/location")
    public void updateLocation(ShipperLocationRequest request, Principal principal) {

        if (request.getOrderId() == null) {
            log.warn("[WS] Missing orderId, skipping");
            return;
        }

        // ✅ Shipper tắt GPS → broadcast tín hiệu gpsOff ngay, không cần update DB
        if (Boolean.TRUE.equals(request.getGpsOff())) {
            log.info("[WS] Shipper GPS OFF for order: {}", request.getOrderId());
            messagingTemplate.convertAndSend(
                    "/topic/order/" + request.getOrderId() + "/location",
                    java.util.Map.of("gpsOff", true)
            );
            return;
        }

        // Validate bình thường khi không phải gpsOff
        if (request.getLatitude() == null || request.getLongitude() == null) {
            log.warn("[WS] Missing lat/lng for order {}, skipping", request.getOrderId());
            return;
        }

        String email = principal != null ? principal.getName() : null;
        if (email == null) {
            log.warn("[WS] No principal found, skipping");
            return;
        }

        log.info("[WS] Location update for order: {}, lat={}, lng={}",
                request.getOrderId(), request.getLatitude(), request.getLongitude());

        ShipperLocationResponse response = shipperService.updateLocationByEmail(email, request);

        messagingTemplate.convertAndSend(
                "/topic/order/" + request.getOrderId() + "/location",
                response
        );
        log.info("[WS] Broadcasted to /topic/order/{}/location", request.getOrderId());
    }
}