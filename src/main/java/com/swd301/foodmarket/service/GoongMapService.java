package com.swd301.foodmarket.service;

import com.swd301.foodmarket.dto.goong.LatLong;

public interface GoongMapService {

    public LatLong geocodeAddress(String address);
    public double getDistanceKm(LatLong origin, LatLong destination);

}
