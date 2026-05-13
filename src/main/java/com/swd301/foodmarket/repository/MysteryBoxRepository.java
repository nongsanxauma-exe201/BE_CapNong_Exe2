package com.swd301.foodmarket.repository;

import com.swd301.foodmarket.entity.MysteryBox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

// repository/MysteryBoxRepository.java
public interface MysteryBoxRepository extends JpaRepository<MysteryBox, Integer> {
    List<MysteryBox> findByShopOwner_Id(Integer shopOwnerId);

    @Query("""
       SELECT b
       FROM MysteryBox b
       LEFT JOIN FETCH b.productMysteries pm
       LEFT JOIN FETCH pm.product
       WHERE b.id = :id
       """)
    Optional<MysteryBox> findByIdWithProducts(Integer id);


}
