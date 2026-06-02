package com.swd301.foodmarket.repository;

import com.swd301.foodmarket.entity.Order;
import com.swd301.foodmarket.entity.Tracking;
import com.swd301.foodmarket.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.UUID;

public interface TrackingRepository extends JpaRepository<Tracking, UUID> {
    Integer countByCreatedAtBetweenAndStatus(LocalDateTime createdAtAfter, LocalDateTime createdAtBefore, OrderStatus status);
}
