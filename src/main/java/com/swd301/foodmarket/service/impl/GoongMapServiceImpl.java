package com.swd301.foodmarket.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swd301.foodmarket.dto.goong.*;
import com.swd301.foodmarket.dto.response.AddressSuggestionResponse;
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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class GoongMapServiceImpl implements GoongMapService {

    RestTemplate restTemplate;
    ObjectMapper objectMapper;

    @NonFinal @Value("${goong.api.key}")
    String goongApiKey;

    @NonFinal @Value("${goong.geocode.url}")
    String geocodeUrl;

    @NonFinal @Value("${goong.distance.matrix.url}")
    String distanceMatrixUrl;

    @NonFinal @Value("${goong.places.autocomplete.url}")
    String placesAutocompleteUrl;

    @NonFinal @Value("${goong.places.detail.url}")
    String placesDetailUrl;

    private HttpEntity<String> buildRequest() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "Mozilla/5.0");
        headers.set("Accept", "application/json");
        return new HttpEntity<>(headers);
    }

    // ── Public API ──────────────────────────────────────────────────────────

    /**
     * Chuyển địa chỉ text → toạ độ lat/lng.
     *
     * - Địa chỉ ngắn/tìm kiếm (< 2 dấu phẩy): dùng Places AutoComplete (tốt cho partial search)
     * - Địa chỉ đầy đủ (≥ 2 dấu phẩy): dùng Geocode API trực tiếp (AutoComplete trả kết quả sai
     *   khi nhận chuỗi địa chỉ hoàn chỉnh vì nó hiểu là "tìm business" thay vì geocode địa điểm)
     */
    @Override
    public LatLong geocodeAddress(String address) {
        long commas = address.chars().filter(c -> c == ',').count();

        if (commas >= 2) {
            // Địa chỉ đầy đủ → Geocode API chính xác hơn
            log.info("[Goong] Địa chỉ đầy đủ ({} dấu phẩy) → dùng Geocode API", commas);
            LatLong result = geocodeByGeocode(address);
            if (result != null) return result;
            // Fallback về Places nếu Geocode thất bại
            log.info("[Goong] Geocode thất bại, thử Places AutoComplete");
            return geocodeByPlaces(address);
        } else {
            // Địa chỉ ngắn → Places AutoComplete tốt hơn
            log.info("[Goong] Địa chỉ ngắn → dùng Places AutoComplete");
            LatLong result = geocodeByPlaces(address);
            if (result != null) return result;
            log.info("[Goong] Places thất bại, thử Geocode API");
            return geocodeByGeocode(address);
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

            ResponseEntity<GoongDistanceMatrixResponse> resp =
                    restTemplate.exchange(url, HttpMethod.GET, buildRequest(), GoongDistanceMatrixResponse.class);

            GoongDistanceMatrixResponse body = resp.getBody();
            if (body == null || body.getRows() == null || body.getRows().isEmpty()) {
                log.warn("[Goong] Distance Matrix không trả về kết quả");
                return Double.MAX_VALUE;
            }

            Element element = body.getRows().get(0).getElements().get(0);
            if (!"OK".equals(element.getStatus()) || element.getDistance() == null) {
                log.warn("[Goong] Distance Matrix element status: {}", element.getStatus());
                return Double.MAX_VALUE;
            }

            double km = element.getDistance().getValue() / 1000.0;
            log.info("[Goong] Distance ({},{}) → ({},{}) = {} km",
                    origin.getLatitude(), origin.getLongitude(),
                    destination.getLatitude(), destination.getLongitude(), km);
            return km;

        } catch (Exception e) {
            log.error("[Goong] Distance Matrix lỗi: {}", e.getMessage());
            return Double.MAX_VALUE;
        }
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    /**
     * Bước 1: Places AutoComplete → lấy place_id của kết quả đầu tiên.
     * Bước 2: Place Detail → lấy lat/lng chính xác từ place_id đó.
     */
    // Toạ độ trung tâm TPHCM để bias kết quả Places AutoComplete
    private static final double HCM_LAT = 10.7769;
    private static final double HCM_LNG = 106.7009;
    private static final int    HCM_RADIUS_METERS = 50_000; // 50 km bao phủ toàn TP

    private LatLong geocodeByPlaces(String address) {
        try {
            String encoded = URLEncoder.encode(address, StandardCharsets.UTF_8);
            // Thêm location bias về TPHCM để địa chỉ ngắn vẫn tìm đúng khu vực
            String acUrl = placesAutocompleteUrl
                    + "?input=" + encoded
                    + "&location=" + HCM_LAT + "," + HCM_LNG
                    + "&radius=" + HCM_RADIUS_METERS
                    + "&api_key=" + goongApiKey;

            log.info("[Goong][AutoComplete] URL: {}", acUrl);

            ResponseEntity<String> rawResp =
                    restTemplate.exchange(acUrl, HttpMethod.GET, buildRequest(), String.class);

            log.info("[Goong][AutoComplete] RAW: {}", rawResp.getBody());

            GoongAutoCompleteResponse acResp =
                    objectMapper.readValue(rawResp.getBody(), GoongAutoCompleteResponse.class);

            if (!"OK".equals(acResp.getStatus())
                    || acResp.getPredictions() == null
                    || acResp.getPredictions().isEmpty()) {
                log.warn("[Goong][AutoComplete] status={} — không có kết quả cho: {}", acResp.getStatus(), address);
                return null;
            }

            String placeId = acResp.getPredictions().get(0).getPlaceId();
            String description = acResp.getPredictions().get(0).getDescription();
            log.info("[Goong][AutoComplete] '{}' → description='{}' placeId='{}'", address, description, placeId);

            return geocodeByPlaceId(placeId);

        } catch (Exception e) {
            log.error("[Goong][AutoComplete] lỗi cho '{}': {}", address, e.getMessage());
            return null;
        }
    }

    @Override
    public LatLong geocodeByPlaceId(String placeId) {
        try {
            String encoded = URLEncoder.encode(placeId, StandardCharsets.UTF_8);
            String detailUrl = placesDetailUrl + "?place_id=" + encoded + "&api_key=" + goongApiKey;

            log.info("[Goong][PlaceDetail] URL: {}", detailUrl);

            ResponseEntity<String> rawResp =
                    restTemplate.exchange(detailUrl, HttpMethod.GET, buildRequest(), String.class);

            log.info("[Goong][PlaceDetail] RAW: {}", rawResp.getBody());

            GoongPlaceDetailResponse detailResp =
                    objectMapper.readValue(rawResp.getBody(), GoongPlaceDetailResponse.class);

            if (!"OK".equals(detailResp.getStatus()) || detailResp.getResult() == null) {
                log.warn("[Goong][PlaceDetail] status={} — không có kết quả cho placeId={}", detailResp.getStatus(), placeId);
                return null;
            }

            Location loc = detailResp.getResult().getGeometry().getLocation();
            double lat = loc.getLat();
            double lng = loc.getLng();

            log.info("[Goong][PlaceDetail] placeId='{}' → lat={}, lng={}", placeId, lat, lng);

            if (!isInVietnam(lat, lng)) {
                log.warn("[Goong][PlaceDetail] Toạ độ NGOÀI Việt Nam lat={} lng={} — bỏ qua", lat, lng);
                return null;
            }

            return new LatLong(lat, lng);

        } catch (Exception e) {
            log.error("[Goong][PlaceDetail] lỗi cho placeId='{}': {}", placeId, e.getMessage());
            return null;
        }
    }

    /** Fallback: Geocode API gốc (ít chính xác hơn với địa chỉ tiếng Việt) */
    private LatLong geocodeByGeocode(String address) {
        try {
            String encoded = URLEncoder.encode(address, StandardCharsets.UTF_8);
            String url = geocodeUrl + "?address=" + encoded + "&api_key=" + goongApiKey;

            log.info("[Goong][Geocode] URL: {}", url);

            ResponseEntity<String> rawResp =
                    restTemplate.exchange(url, HttpMethod.GET, buildRequest(), String.class);

            log.info("[Goong][Geocode] RAW: {}", rawResp.getBody());

            GoongGeocodeResponse resp =
                    objectMapper.readValue(rawResp.getBody(), GoongGeocodeResponse.class);

            if (resp == null || resp.getResults() == null || resp.getResults().isEmpty()) {
                log.warn("[Goong][Geocode] Không tìm được kết quả cho: {}", address);
                return null;
            }

            double lat = resp.getResults().get(0).getGeometry().getLocation().getLat();
            double lng = resp.getResults().get(0).getGeometry().getLocation().getLng();

            log.info("[Goong][Geocode] '{}' → lat={}, lng={}", address, lat, lng);

            if (!isInVietnam(lat, lng)) {
                log.warn("[Goong][Geocode] Toạ độ NGOÀI Việt Nam lat={} lng={} — bỏ qua", lat, lng);
                return null;
            }

            return new LatLong(lat, lng);

        } catch (Exception e) {
            log.error("[Goong][Geocode] lỗi cho '{}': {}", address, e.getMessage());
            return null;
        }
    }

    @Override
    public List<AddressSuggestionResponse> getAddressSuggestions(String input) {
        try {
            String encoded = URLEncoder.encode(input, StandardCharsets.UTF_8);
            String url = placesAutocompleteUrl
                    + "?input=" + encoded
                    + "&location=" + HCM_LAT + "," + HCM_LNG
                    + "&radius=" + HCM_RADIUS_METERS
                    + "&api_key=" + goongApiKey;

            ResponseEntity<String> rawResp =
                    restTemplate.exchange(url, HttpMethod.GET, buildRequest(), String.class);

            GoongAutoCompleteResponse acResp =
                    objectMapper.readValue(rawResp.getBody(), GoongAutoCompleteResponse.class);

            if (!"OK".equals(acResp.getStatus())
                    || acResp.getPredictions() == null
                    || acResp.getPredictions().isEmpty()) {
                return Collections.emptyList();
            }

            return acResp.getPredictions().stream()
                    .map(p -> AddressSuggestionResponse.builder()
                            .placeId(p.getPlaceId())
                            .description(p.getDescription())
                            .build())
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("[Goong][Suggestions] lỗi cho '{}': {}", input, e.getMessage());
            return Collections.emptyList();
        }
    }

    /** Việt Nam: latitude 8–24°N, longitude 102–110°E */
    private boolean isInVietnam(double lat, double lng) {
        return lat >= 8.0 && lat <= 24.0 && lng >= 102.0 && lng <= 110.0;
    }
}
