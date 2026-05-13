package com.swd301.foodmarket.dto.goong;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GoongGeocodeResponse {
    @JsonProperty("status")
    private String status;

    @JsonProperty("results")
    private List<GoongGeocodeResult> results;
}