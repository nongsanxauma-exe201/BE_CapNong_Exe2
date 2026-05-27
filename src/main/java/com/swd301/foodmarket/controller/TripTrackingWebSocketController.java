package com.swd301.foodmarket.controller;

import com.swd301.foodmarket.dto.request.TripTrackingRequest;
import com.swd301.foodmarket.dto.response.TripTrackingResponse;
import com.swd301.foodmarket.service.TripTrackingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
@Slf4j
public class TripTrackingWebSocketController {

    private final TripTrackingService
            tripTrackingService;

    private final SimpMessagingTemplate
            messagingTemplate;

    @MessageMapping("/trip/location")
    public void updateLocation(
            TripTrackingRequest request,
            Principal principal
    ) {
        log.info("Received location update for trip {}: lat={}, lng={}, speed={}, heading={}",
            request.getTripId(), request.getLat(), request.getLng(), request.getSpeed(), request.getHeading());
        TripTrackingResponse response =
                tripTrackingService
                        .updateLocation(
                                request,
                                principal.getName()
                        );

        messagingTemplate.convertAndSend(
                "/topic/trip/"
                        + request.getTripId()
                        + "/location",
                response
        );
    }
}
