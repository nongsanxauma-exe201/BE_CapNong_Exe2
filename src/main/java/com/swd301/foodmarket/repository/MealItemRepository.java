package com.swd301.foodmarket.repository;

import com.swd301.foodmarket.entity.MealItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MealItemRepository extends JpaRepository<MealItem, Integer> {
}