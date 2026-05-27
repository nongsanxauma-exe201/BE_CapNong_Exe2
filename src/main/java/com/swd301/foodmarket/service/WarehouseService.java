package com.swd301.foodmarket.service;

import com.swd301.foodmarket.dto.request.WarehouseCreateRequest;
import com.swd301.foodmarket.entity.Warehouse;

import java.util.List;

public interface WarehouseService {
    Warehouse create(WarehouseCreateRequest request );
    List<Warehouse> getAll();
}
