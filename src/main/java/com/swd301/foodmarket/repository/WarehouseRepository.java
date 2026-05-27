package com.swd301.foodmarket.repository;

import com.swd301.foodmarket.entity.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WarehouseRepository extends JpaRepository<Warehouse, Integer> {
    List<Warehouse> findAllByActive(boolean active);
}
