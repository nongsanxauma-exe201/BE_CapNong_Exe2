package com.swd301.foodmarket.service;

import com.swd301.foodmarket.dto.request.VehicleCreateRequest;
import com.swd301.foodmarket.dto.request.VehicleUpdateRequest;
import com.swd301.foodmarket.dto.response.VehicleResponse;

import java.util.List;

public interface VehicleService {

    VehicleResponse create(VehicleCreateRequest request);

    List<VehicleResponse> getMyVehicles();

    VehicleResponse getById(Integer id);

    VehicleResponse update(Integer id, VehicleUpdateRequest request);

    void delete(Integer id);

}
