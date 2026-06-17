package com.swd301.foodmarket.dto.internal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AiLlmItemPayload {

    private String name;
    private Double quantity;
    private String unit;
}
