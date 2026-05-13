package com.swd301.foodmarket.repository;

import com.swd301.foodmarket.entity.OtpVerification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OtpVerificationRepository extends JpaRepository<OtpVerification, Integer> {
    Optional<OtpVerification> findByEmail(String email);
    void deleteByEmail(String email);
}
