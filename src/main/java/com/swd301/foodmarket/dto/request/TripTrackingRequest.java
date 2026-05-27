package com.swd301.foodmarket.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TripTrackingRequest {

    Integer tripId;

    Double lat;

    Double lng;

    Double speed;

    Double heading;
}