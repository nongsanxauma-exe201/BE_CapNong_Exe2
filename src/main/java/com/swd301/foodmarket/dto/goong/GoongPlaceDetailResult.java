package com.swd301.foodmarket.dto.goong;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GoongPlaceDetailResult {
    private Geometry geometry;

    @JsonProperty("formatted_address")
    private String formattedAddress;
}
