package com.swd301.foodmarket.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AddressSuggestionResponse {
    @JsonProperty("placeId")
    private String placeId;

    @JsonProperty("description")
    private String description;
}
