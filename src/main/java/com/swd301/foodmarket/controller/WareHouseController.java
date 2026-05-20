package com.swd301.foodmarket.controller;

import com.swd301.foodmarket.dto.request.WarehouseCreateRequest;
import com.swd301.foodmarket.dto.response.ApiResponse;
import com.swd301.foodmarket.entity.Warehouse;
import com.swd301.foodmarket.service.WarehouseService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/warehouses")
@RequiredArgsConstructor
public class WareHouseController {
    private final WarehouseService warehouseService;
    @PostMapping
    public ApiResponse<Warehouse> create(
            @RequestBody WarehouseCreateRequest request ) {
        return ApiResponse.<Warehouse>builder().result(warehouseService.create(request)).build();
    }
    @GetMapping
    public ApiResponse<List<Warehouse>> getAll() {
        return ApiResponse.<List<Warehouse>>builder().result(warehouseService.getAll()).build();
    }
}
