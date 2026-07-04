package com.swd301.foodmarket.repository;

import com.swd301.foodmarket.entity.CheckoutBatch;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface CheckoutBatchRepository extends JpaRepository<CheckoutBatch, Integer> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM CheckoutBatch b WHERE b.payosOrderCode = :orderCode")
    Optional<CheckoutBatch> findByPayosOrderCodeForUpdate(String orderCode);

    Optional<CheckoutBatch> findByPayosOrderCode(String orderCode);
}
