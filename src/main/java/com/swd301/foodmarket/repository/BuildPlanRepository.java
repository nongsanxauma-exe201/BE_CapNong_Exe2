package com.swd301.foodmarket.repository;

import com.swd301.foodmarket.entity.BuildPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BuildPlanRepository extends JpaRepository<BuildPlan, Integer> {

    List<BuildPlan> findByBuyer_Id(Integer buyerId);
}