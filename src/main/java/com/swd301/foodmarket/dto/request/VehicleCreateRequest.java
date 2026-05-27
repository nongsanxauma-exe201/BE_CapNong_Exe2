package com.swd301.foodmarket.dto.request;

import lombok.Data;

@Data
public class VehicleCreateRequest {

    private String vehicleNumber;

    private Double capacityKg;

    private String type;

}
