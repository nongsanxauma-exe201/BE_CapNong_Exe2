package com.swd301.foodmarket.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.swd301.foodmarket.entity.InvalidatedToken;

@Repository
public interface InvalidatedTokenRepository
        extends JpaRepository<InvalidatedToken, String> {
}
