package com.swd301.foodmarket.repository;

import com.swd301.foodmarket.entity.BuildPlanItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BuildPlanItemRepository extends JpaRepository<BuildPlanItem, Integer> {

    List<BuildPlanItem> findByBuildPlanId(Integer planId);
}