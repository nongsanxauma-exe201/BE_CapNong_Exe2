package com.swd301.foodmarket.repository;

import com.swd301.foodmarket.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {

    List<ProductImage> findByProductIdOrderByDisplayOrderAsc(Integer productId);

    Optional<ProductImage> findByProductIdAndIsPrimaryTrue(Integer productId);

    int countByProductId(Integer productId);

    Optional<ProductImage> findByIdAndProductId(Long id, Integer productId);
}
