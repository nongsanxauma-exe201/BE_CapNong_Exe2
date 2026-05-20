package com.swd301.foodmarket.dto.request;

import lombok.Data;

@Data

public class WarehouseCreateRequest {

    private String name;

    private String address;

    private Double latitude;

    private Double longitude;

}