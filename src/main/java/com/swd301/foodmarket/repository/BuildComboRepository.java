package com.swd301.foodmarket.repository;

import com.swd301.foodmarket.entity.BuildCombo;
import com.swd301.foodmarket.enums.MealType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BuildComboRepository extends JpaRepository<BuildCombo, Integer> {
    List<BuildCombo> findByShopOwner_Id(Integer shopOwnerId);
    List<BuildCombo> findByMealType(MealType mealType);

}
