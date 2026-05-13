package com.swd301.foodmarket.repository;

import com.swd301.foodmarket.entity.ProductMystery;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// repository/ProductMysteryRepository.java
public interface ProductMysteryRepository extends JpaRepository<ProductMystery, Integer> {
    List<ProductMystery> findByMysteryBox_Id(Integer mysteryId);

    void deleteByMysteryBox_Id(Integer mysteryId);

    List<ProductMystery> findByProduct_Id(Integer productId);
}
