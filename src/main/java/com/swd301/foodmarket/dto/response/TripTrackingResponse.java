package com.swd301.foodmarket.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class TripTrackingResponse {

    Integer tripId;

    Double lat;

    Double lng;

    Double speed;

    Double heading;

    String updatedAt;
}