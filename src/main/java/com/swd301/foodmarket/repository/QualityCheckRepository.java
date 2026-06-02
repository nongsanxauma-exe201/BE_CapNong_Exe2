package com.swd301.foodmarket.repository;

import com.swd301.foodmarket.entity.QualityCheck;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface QualityCheckRepository extends JpaRepository<QualityCheck, UUID> {
}
