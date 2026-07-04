package com.swd301.foodmarket.repository;

import com.swd301.foodmarket.entity.Payment;
import com.swd301.foodmarket.enums.PaymentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Integer> {
    Optional<Payment> findByPayosOrderCode(String payosOrderCode);
    boolean existsByOrder_IdAndStatus(Integer orderId, PaymentStatus status);

    Optional<Payment> findTopByOrderIdAndStatusOrderByCreatedAtDesc(Integer orderId, com.swd301.foodmarket.enums.PaymentStatus status);

    List<Payment> findByStatusAndCreatedAtBefore(com.swd301.foodmarket.enums.PaymentStatus status, LocalDateTime createdAtBefore);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Payment p WHERE p.payosOrderCode = :orderCode")
    Optional<Payment> findByOrderCodeForUpdate(String orderCode);

}