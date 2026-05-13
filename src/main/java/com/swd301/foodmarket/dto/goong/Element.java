package com.swd301.foodmarket.dto.goong;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Element {
    private Distance distance;
}