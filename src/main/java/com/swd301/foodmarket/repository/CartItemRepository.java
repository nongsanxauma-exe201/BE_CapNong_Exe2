package com.swd301.foodmarket.repository;

import com.swd301.foodmarket.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Integer> {

    // Tìm item sản phẩm thường trong giỏ (giữ nguyên)
    Optional<CartItem> findByCartAndProduct(Cart cart, Product product);

    // Tìm item túi mù trong giỏ (mới)
    Optional<CartItem> findByCartAndMysteryBox(Cart cart, MysteryBox mysteryBox);

    // Lấy tất cả item trong giỏ (giữ nguyên)
    List<CartItem> findByCart(Cart cart);

    Optional<CartItem> findByCartAndBuildCombo(Cart cart, BuildCombo buildCombo);

    @Modifying
    @Transactional
    void deleteAllByCart(Cart cart);
}