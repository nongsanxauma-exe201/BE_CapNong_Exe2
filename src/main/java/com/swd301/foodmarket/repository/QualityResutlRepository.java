package com.swd301.foodmarket.repository;

import com.swd301.foodmarket.entity.QualityResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface QualityResutlRepository extends JpaRepository<QualityResult, UUID> {
}
