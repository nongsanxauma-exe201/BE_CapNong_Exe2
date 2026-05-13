package com.swd301.foodmarket.repository;

import com.swd301.foodmarket.entity.Cart;
import com.swd301.foodmarket.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart,Integer> {
    Optional<Cart> findByUser(User user);
}
