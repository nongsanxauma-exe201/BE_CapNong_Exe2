package com.swd301.foodmarket.controller;

import com.swd301.foodmarket.dto.request.VehicleCreateRequest;
import com.swd301.foodmarket.dto.request.VehicleUpdateRequest;
import com.swd301.foodmarket.dto.response.ApiResponse;
import com.swd301.foodmarket.dto.response.UserResponse;
import com.swd301.foodmarket.dto.response.VehicleResponse;
import com.swd301.foodmarket.service.VehicleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/vehicles")
@RequiredArgsConstructor
public class VehicleController {

    private final VehicleService vehicleService;

    @PostMapping
    public ApiResponse<VehicleResponse> create(
            @RequestBody VehicleCreateRequest request
    ) {
        return  ApiResponse.<VehicleResponse>builder().result(vehicleService.create(request)).build();
    }

    @GetMapping("/my")
    public ApiResponse<List<VehicleResponse>> getMyVehicles() {
        return  ApiResponse.<List<VehicleResponse>>builder().result(vehicleService.getMyVehicles()).build();
    }

    @GetMapping("/{id}")
    public ApiResponse<VehicleResponse> getById(
            @PathVariable Integer id
    ) {
        return  ApiResponse.<VehicleResponse>builder().result(vehicleService.getById(id)).build();
    }

    @PutMapping("/{id}")
    public ApiResponse<VehicleResponse> update(
            @PathVariable Integer id,
            @RequestBody VehicleUpdateRequest request
    ) {
        return ApiResponse.<VehicleResponse>builder().result(vehicleService.update(id, request)).build();
    }

    @DeleteMapping("/{id}")
    public void delete(
            @PathVariable Integer id
    ) {
        vehicleService.delete(id);
    }

}