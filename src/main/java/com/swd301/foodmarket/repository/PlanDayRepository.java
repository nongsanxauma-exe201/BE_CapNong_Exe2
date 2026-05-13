package com.swd301.foodmarket.repository;

import com.swd301.foodmarket.entity.PlanDay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlanDayRepository extends JpaRepository<PlanDay, Integer> {
}