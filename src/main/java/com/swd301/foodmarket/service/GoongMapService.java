package com.swd301.foodmarket.service;

import com.swd301.foodmarket.dto.goong.LatLong;
import com.swd301.foodmarket.dto.response.AddressSuggestionResponse;

import java.util.List;

public interface GoongMapService {

    LatLong geocodeAddress(String address);

    /** Dùng khi đã biết placeId (từ suggestion) — bỏ qua AutoComplete, gọi Place Detail trực tiếp. */
    LatLong geocodeByPlaceId(String placeId);

    double getDistanceKm(LatLong origin, LatLong destination);

    List<AddressSuggestionResponse> getAddressSuggestions(String input);
}
