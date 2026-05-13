package com.swd301.foodmarket.repository;

import com.swd301.foodmarket.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Integer> {
    List<Review> findByShopOwnerId(Integer shopOwnerId);
    Page<Review> findAll(Pageable pageable);
    Page<Review> findByShopOwnerId(Integer shopOwnerId, Pageable pageable);

    List<Review> findByOrderDetailProductId(Integer productId);

    boolean existsByOrderDetailId(Integer orderDetailId);

    boolean existsByMysteryBoxId(Integer mysteryBoxId);
}
