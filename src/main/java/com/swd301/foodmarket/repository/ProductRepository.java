package com.swd301.foodmarket.repository;

import com.swd301.foodmarket.entity.OrderDetail;
import com.swd301.foodmarket.entity.Product;
import com.swd301.foodmarket.entity.User;
import com.swd301.foodmarket.enums.OrderStatus;
import com.swd301.foodmarket.enums.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Integer> {
    List<Product> findByShopOwnerId(Integer ownerId);

    Page<Product> findAll(Pageable pageable);

    Page<Product> findByShopOwnerId(Integer ownerId, Pageable pageable);

    List<Product> findByExpiryDateBeforeAndStatusNot(LocalDate date, ProductStatus status);

    List<Product> findByProductNameContainingIgnoreCaseAndStatus(String keyword, ProductStatus status);

    List<Product> findByStatusOrderByCategoryAsc(ProductStatus status);

    List<Product> findByStatus(ProductStatus status);

    Page<Product> findAllByStatus(ProductStatus status, Pageable pageable);

    @Query("""
    SELECT p
    FROM Product p
    WHERE p.shopOwner = :shopOwner
    ORDER BY COALESCE((
        SELECT SUM(od.quantity)
        FROM OrderDetail od
        WHERE od.product = p
          AND od.order.status = :status
          AND od.order.createdAt >= :from
          AND od.order.createdAt < :to
    ), 0) DESC
""")
    Page<Product> topSold(
            @Param("shopOwner") User shopOwner,
            @Param("status") OrderStatus status,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable
    );

    Page<Product> findAllByStatusAndShopOwner_ShopActive(ProductStatus status, boolean shopOwnerShopActive, Pageable pageable);

    List<Product> findAllByStatusAndShopOwner_ShopActive(ProductStatus status, boolean shopOwnerShopActive);
}

