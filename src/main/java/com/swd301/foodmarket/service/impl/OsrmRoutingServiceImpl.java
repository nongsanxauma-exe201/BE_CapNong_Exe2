package com.swd301.foodmarket.service.impl;

import com.swd301.foodmarket.dto.response.OsrmRouteResponse;
import com.swd301.foodmarket.dto.response.RouteData;
import com.swd301.foodmarket.service.RoutingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class OsrmRoutingServiceImpl
        implements RoutingService {

    private final RestTemplate restTemplate;

    @Override
    public RouteData getRoute(
            Double startLat,
            Double startLng,
            Double endLat,
            Double endLng
    ) {

        try {

            String url =
                    "https://router.project-osrm.org/route/v1/driving/"
                            + startLng + "," + startLat + ";"
                            + endLng + "," + endLat
                            + "?overview=full&geometries=polyline";

            ResponseEntity<OsrmRouteResponse> response =
                    restTemplate.exchange(
                            url,
                            HttpMethod.GET,
                            null,
                            OsrmRouteResponse.class
                    );

            if (response.getBody() == null
                    || response.getBody().getRoutes() == null
                    || response.getBody().getRoutes().isEmpty()) {

                return null;
            }

            var route =
                    response.getBody().getRoutes().get(0);

            return RouteData.builder()
                    .polyline(route.getGeometry())
                    .distanceKm(route.getDistance() / 1000)
                    .durationSeconds(
                            route.getDuration().longValue()
                    )
                    .build();

        } catch (Exception e) {

            throw new RuntimeException(
                    "Cannot calculate route"
            );
        }
    }
}