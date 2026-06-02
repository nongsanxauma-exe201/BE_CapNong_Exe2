package com.swd301.foodmarket.dto.response;

import lombok.Data;

@Data
public class VehicleResponse {

    private Integer id;

    private String vehicleNumber;

    private Double capacityKg;

    private String type;

    private UserResponse owner;

    private boolean isAvailable;
}