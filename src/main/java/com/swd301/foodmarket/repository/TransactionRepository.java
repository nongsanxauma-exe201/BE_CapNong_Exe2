package com.swd301.foodmarket.repository;

import com.swd301.foodmarket.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction, Integer> {
}
