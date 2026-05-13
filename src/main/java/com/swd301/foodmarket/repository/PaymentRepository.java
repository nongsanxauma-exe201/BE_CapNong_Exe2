package com.swd301.foodmarket.repository;

import com.swd301.foodmarket.entity.Payment;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Integer> {
    Optional<Payment> findByPayosOrderCode(String payosOrderCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Payment p WHERE p.payosOrderCode = :orderCode")
    Optional<Payment> findByOrderCodeForUpdate(String orderCode);

}