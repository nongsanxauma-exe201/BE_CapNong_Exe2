package com.swd301.foodmarket.repository;

import com.swd301.foodmarket.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category,Long> {
}
