package com.swd301.foodmarket.repository;

import com.swd301.foodmarket.entity.VoucherUsage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;
public interface VoucherUsageRepository  extends JpaRepository<VoucherUsage, UUID> {
    Optional<VoucherUsage> findByOrder_Id(Integer orderId);
}
