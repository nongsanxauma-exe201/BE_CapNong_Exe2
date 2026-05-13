package com.swd301.foodmarket.dto.goong;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

// ✅ Bỏ qua các field Goong trả về mà mình không dùng
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GoongGeocodeResult {

    @JsonProperty("formatted_address")
    private String formattedAddress;

    @JsonProperty("geometry")
    private Geometry geometry;
}