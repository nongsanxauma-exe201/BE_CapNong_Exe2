package com.swd301.foodmarket.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swd301.foodmarket.dto.goong.*;
import com.swd301.foodmarket.service.GoongMapService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class GoongMapServiceImpl implements GoongMapService {

    RestTemplate restTemplate;

    @NonFinal
    @Value("${goong.api.key}")
    String goongApiKey;

    @NonFinal
    @Value("${goong.geocode.url}")
    String geocodeUrl;

    @NonFinal
    @Value("${goong.distance.matrix.url}")
    String distanceMatrixUrl;

    private HttpEntity<String> buildRequestEntity() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "Mozilla/5.0");
        headers.set("Accept", "application/json");
        return new HttpEntity<>(headers);
    }

    @Override
    public LatLong geocodeAddress(String address) {
        try {
            // ✅ Fix: dùng %20 thay vì + để Goong không hiểu sai địa chỉ tiếng Việt
            String url = org.springframework.web.util.UriComponentsBuilder
                    .fromHttpUrl(geocodeUrl)
                    .queryParam("address", address)
                    .queryParam("api_key", goongApiKey)
                    .build()
                    .toUriString();

            log.info("[Goong] Raw URL: {}", url);

            ResponseEntity<String> rawResponse =
                    restTemplate.exchange(url, HttpMethod.GET, buildRequestEntity(), String.class);

            String rawJson = rawResponse.getBody();

            // ✅ Parse với ObjectMapper (đã có @JsonIgnoreProperties trên DTO)
            ObjectMapper mapper = new ObjectMapper();
            GoongGeocodeResponse response = mapper.readValue(rawJson, GoongGeocodeResponse.class);

            if (response != null && response.getResults() != null && !response.getResults().isEmpty()) {
                GoongGeocodeResult result = response.getResults().get(0);
                double lat = result.getGeometry().getLocation().getLat();
                double lng = result.getGeometry().getLocation().getLng();
                log.info("[Goong] Geocode '{}' → ({}, {})", address, lat, lng);
                return new LatLong(lat, lng);
            }

            log.warn("[Goong] Geocode không tìm được kết quả cho: {}", address);
            return null;

        } catch (Exception e) {
            log.error("[Goong] Geocode lỗi cho địa chỉ '{}': {}", address, e.getMessage());
            return null;
        }
    }

    @Override
    public double getDistanceKm(LatLong origin, LatLong destination) {
        try {
            String url = String.format(
                    "%s?origins=%s,%s&destinations=%s,%s&api_key=%s",
                    distanceMatrixUrl,
                    origin.getLatitude(), origin.getLongitude(),
                    destination.getLatitude(), destination.getLongitude(),
                    goongApiKey
            );

            ResponseEntity<GoongDistanceMatrixResponse> responseEntity =
                    restTemplate.exchange(url, HttpMethod.GET, buildRequestEntity(), GoongDistanceMatrixResponse.class);

            GoongDistanceMatrixResponse response = responseEntity.getBody();

            if (response != null
                    && response.getRows() != null
                    && !response.getRows().isEmpty()
                    && response.getRows().get(0).getElements() != null
                    && !response.getRows().get(0).getElements().isEmpty()) {

                double meters = response.getRows().get(0).getElements().get(0).getDistance().getValue();
                double km = meters / 1000.0;
                log.info("[Goong] Distance ({},{}) → ({},{}) = {} km",
                        origin.getLatitude(), origin.getLongitude(),
                        destination.getLatitude(), destination.getLongitude(), km);
                return km;
            }

            log.warn("[Goong] Distance Matrix không trả về kết quả");
            return Double.MAX_VALUE;

        } catch (Exception e) {
            log.error("[Goong] Distance Matrix lỗi: {}", e.getMessage());
            return Double.MAX_VALUE;
        }
    }
}