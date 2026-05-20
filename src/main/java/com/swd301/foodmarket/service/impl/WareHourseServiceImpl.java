package com.swd301.foodmarket.service.impl;

import com.swd301.foodmarket.dto.request.WarehouseCreateRequest;
import com.swd301.foodmarket.entity.Warehouse;
import com.swd301.foodmarket.repository.WarehouseRepository;
import com.swd301.foodmarket.service.WarehouseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
@Service
@RequiredArgsConstructor
public class WareHourseServiceImpl implements WarehouseService {
    private final WarehouseRepository warehouseRepository;

    @Override
    public Warehouse create(WarehouseCreateRequest request) {
        Warehouse warehouse = Warehouse.builder()
                .name(request.getName())
                .address(request.getAddress())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .build();
        warehouseRepository.save(warehouse);
        return warehouse;
    }

    @Override
    public List<Warehouse> getAll() {
        return warehouseRepository.findAllByActive(true);
    }
}
