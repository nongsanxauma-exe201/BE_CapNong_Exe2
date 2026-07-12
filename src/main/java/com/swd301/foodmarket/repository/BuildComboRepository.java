package com.swd301.foodmarket.repository;

import com.swd301.foodmarket.entity.BuildCombo;
import com.swd301.foodmarket.enums.MealType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BuildComboRepository extends JpaRepository<BuildCombo, Integer> {
    List<BuildCombo> findByShopOwner_Id(Integer shopOwnerId);
    List<BuildCombo> findByMealType(MealType mealType);

    @Query("""
        SELECT DISTINCT c FROM BuildCombo c
        LEFT JOIN FETCH c.items items
        LEFT JOIN FETCH items.product
        WHERE c.id = :id
    """)
    Optional<BuildCombo> findByIdWithItems(@Param("id") Integer id);
}
