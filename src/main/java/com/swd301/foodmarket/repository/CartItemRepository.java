package com.swd301.foodmarket.repository;

import com.swd301.foodmarket.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Integer> {

    Optional<CartItem> findByCartAndProduct(Cart cart, Product product);

    Optional<CartItem> findByCartAndMysteryBox(Cart cart, MysteryBox mysteryBox);

    List<CartItem> findByCart(Cart cart);

    @Query("""
        SELECT DISTINCT ci FROM CartItem ci
        LEFT JOIN FETCH ci.product p
        LEFT JOIN FETCH p.shopOwner
        LEFT JOIN FETCH ci.mysteryBox mb
        LEFT JOIN FETCH mb.shopOwner
        LEFT JOIN FETCH ci.buildCombo bc
        LEFT JOIN FETCH bc.shopOwner
        LEFT JOIN FETCH bc.items bci
        LEFT JOIN FETCH bci.product cp
        LEFT JOIN FETCH cp.shopOwner
        WHERE ci.cart = :cart
    """)
    List<CartItem> findByCartWithDetails(@Param("cart") Cart cart);

    Optional<CartItem> findByCartAndBuildCombo(Cart cart, BuildCombo buildCombo);

    @Modifying
    @Transactional
    void deleteAllByCart(Cart cart);
}
