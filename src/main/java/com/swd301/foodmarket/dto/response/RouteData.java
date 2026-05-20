package com.swd301.foodmarket.dto.response;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RouteData {

    private String polyline;

    private Double distanceKm;

    private Long durationSeconds;
}
