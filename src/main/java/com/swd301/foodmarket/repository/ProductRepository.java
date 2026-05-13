package com.swd301.foodmarket.repository;

import com.swd301.foodmarket.entity.Product;
import com.swd301.foodmarket.enums.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product,Integer> {
    List<Product> findByShopOwnerId(Integer ownerId);
    Page<Product> findAll(Pageable pageable);
    Page<Product> findByShopOwnerId(Integer ownerId, Pageable pageable);
    List<Product> findByExpiryDateBeforeAndStatusNot(LocalDate date, ProductStatus status);
    List<Product> findByProductNameContainingIgnoreCaseAndStatus(String keyword, ProductStatus status);
    List<Product> findByStatusOrderByCategoryAsc(ProductStatus status);
    List<Product> findByStatus(ProductStatus status);
}

