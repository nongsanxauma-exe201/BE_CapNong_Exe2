package com.swd301.foodmarket.repository;

import com.swd301.foodmarket.entity.Review;
import com.swd301.foodmarket.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Integer> {
    List<Review> findByShopOwnerId(Integer shopOwnerId);
    Page<Review> findAll(Pageable pageable);
    Page<Review> findByShopOwnerId(Integer shopOwnerId, Pageable pageable);

    List<Review> findByOrderDetailProductId(Integer productId);

    List<Review> findByMysteryBoxId(Integer mysteryBoxId);

    List<Review> findByBuyerId(Integer buyerId);

    boolean existsByOrderDetailId(Integer orderDetailId);

    boolean existsByMysteryBoxId(Integer mysteryBoxId);

    boolean existsByMysteryBoxIdAndBuyerId(Integer mysteryBoxId, Integer buyerId);

    Long countByShopOwner(User shopOwner);

    @Query("SELECT AVG(r.ratingStar) FROM Review r WHERE r.shopOwner = :shopOwner")
    Double getAverageRatingByShopOwner(@Param("shopOwner") User shopOwner);
    @Query("""
    SELECT COALESCE(AVG(r.ratingStar), 0)
    FROM Review r
    WHERE r.shopOwner = :shopOwner
      AND r.createAt >= :from
      AND r.createAt < :to
""")
    Double getAverageRatingByShopOwnerByDate(
            @Param("shopOwner") User shopOwner,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    long countByShopOwnerAndRatingStar(User shopOwner, BigDecimal ratingStar);
}
