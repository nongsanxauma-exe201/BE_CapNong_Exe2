package com.swd301.foodmarket.repository;

import com.swd301.foodmarket.entity.Order;
import com.swd301.foodmarket.entity.User;
import com.swd301.foodmarket.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Integer> {

    List<Order> findByShipperIsNullAndStatus(OrderStatus status);
    Page<Order> findByShipperIsNullAndStatus(OrderStatus status, Pageable pageable);

    List<Order> findByShipper(User shipper);
    List<Order> findByBuyerId(Integer buyerId);

    long countByBuyerIdAndStatus(Integer buyerId, OrderStatus status);

    Page<Order> findAll(Pageable pageable);
    Page<Order> findByBuyerId(Integer buyerId, Pageable pageable);
    Page<Order> findByBuyerIdAndStatusIn(Integer buyerId, List<OrderStatus> statuses, Pageable pageable);

    List<Order> findByShipperAndStatus(User shipper, OrderStatus status);

    @Query("""
    SELECT DISTINCT o FROM Order o
    WHERE (o.buyer = :userA AND o.shopOwner = :userB)
       OR (o.buyer = :userB AND o.shopOwner = :userA)
    ORDER BY o.createdAt DESC
""")
    List<Order> findOrdersBetweenUsers(@Param("userA") User userA,
                                       @Param("userB") User userB);

    Page<Order> findAllByStatusIsIn(Collection<OrderStatus> statuses, Pageable pageable);

    Integer countByStatus(OrderStatus status);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.status IN :statuses")
    Integer countByStatusIn(@Param("statuses") List<OrderStatus> statuses);

    @Query("""
    SELECT COUNT(DISTINCT o.shopOwner.id)
    FROM Order o
    WHERE o.status = :status
""")
    Integer countDistinctShopOwnerIdByStatus(@Param("status") OrderStatus status);

    Integer countDistinctShopOwnerIdByStatusIsIn(Collection<OrderStatus> statuses);

    Page<Order> findByShopOwner(User shopOwner, Pageable pageable);
    Page<Order> findByShopOwnerAndStatusIn(User shopOwner, List<OrderStatus> statuses, Pageable pageable);
    @Query("""
    SELECT o
    FROM Order o
    WHERE o.status = :status AND
          ( o.shopOwner = :shopOwner) AND
          (:orderIds IS NULL OR o.id NOT IN :orderIds)
""")
    Page<Order> getOrderForTrip(
            @Param("shopOwner") User shopOwner,
            @Param("status") OrderStatus status,
            @Param("orderIds") List<Integer> orderIds, // Đổi từ List<Order> thành List<Integer>
            Pageable pageable
    );
    // Dùng bởi StaffServiceImpl để kiểm tra shipper đang giao đơn nào
    Optional<Order> findFirstByShipperIdAndStatus(Integer shipperId, OrderStatus status);

    List<Order> findByShipperIdAndStatus(Integer shipperId, OrderStatus status);

    Long countAllByShopOwner(User shopOwner);

    Long countAllByShopOwnerAndStatusAndCreatedAtBetween(User shopOwner, OrderStatus status, LocalDateTime createdAtAfter, LocalDateTime createdAtBefore);

    Long countAllByShopOwnerAndStatusIsInAndCreatedAtBetweenAndCreatedAtBetween(User shopOwner, Collection<OrderStatus> statuses, LocalDateTime createdAtAfter, LocalDateTime createdAtBefore, LocalDateTime createdAtAfter1, LocalDateTime createdAtBefore1);

    Long countAllByShopOwnerAndStatusIsInAndCreatedAtBetween(User shopOwner, Collection<OrderStatus> statuses, LocalDateTime createdAtAfter, LocalDateTime createdAtBefore);

    Long countAllByShopOwnerAndCreatedAtBetween(User shopOwner, LocalDateTime createdAtAfter, LocalDateTime createdAtBefore);
    @Query("""
    SELECT COALESCE(SUM(o.totalAmount - o.shippingFee- COALESCE(o.refundAmount, 0)), 0)
    FROM Order o
    WHERE o.shopOwner = :shop
      AND o.createdAt >= :from
      AND o.createdAt < :to
      AND o.status = :status
""")
    Double sumPriceByShop(
            @Param("shop") User shop,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("status") OrderStatus status
    );


    @Query("""
    SELECT COUNT(o)
    FROM Order o
    WHERE o.shopOwner = :shopOwner
      AND o.createdAt >= :from
      AND o.createdAt < :to
""")
    Long countByShopOwnerFromTo(
            @Param("shopOwner") User shopOwner,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    @Query("""
    SELECT COUNT(o)
    FROM Order o
    WHERE o.shopOwner = :shopOwner
      AND o.status = :status
      AND o.createdAt >= :from
      AND o.createdAt < :to
""")
    Long countByShopOwnerAndStatusFromTo(
            @Param("shopOwner") User shopOwner,
            @Param("status") OrderStatus status,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    @Query("""
    SELECT COUNT(o)
    FROM Order o
    WHERE o.shopOwner = :shopOwner
      AND o.status IN :statuses
      AND o.createdAt >= :from
      AND o.createdAt < :to
""")
    Long countByShopOwnerAndStatusInFromTo(
            @Param("shopOwner") User shopOwner,
            @Param("statuses") Collection<OrderStatus> statuses,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    Long countAllByShopOwnerAndStatus(User shopOwner, OrderStatus status);


    List<Order>  findByPaymentMethodIgnoreCaseAndStatusAndCodSettled(String paymentMethod, OrderStatus status, Boolean codSettled);
}
