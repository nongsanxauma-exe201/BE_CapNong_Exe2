package com.swd301.foodmarket.repository;

import com.swd301.foodmarket.entity.Order;
import com.swd301.foodmarket.entity.OrderDetail;
import com.swd301.foodmarket.entity.QualityResult;
import com.swd301.foodmarket.entity.User;
import com.swd301.foodmarket.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderDetailRepository extends JpaRepository<OrderDetail, Integer> {
    Integer countByOrder_Status(OrderStatus orderStatus);


    Integer countByOrderAndQualityResultNull(Order order);

    @Query("""
                SELECT COALESCE(SUM(od.quantity), 0)
                FROM OrderDetail od
                WHERE od.product.id = :id
                  AND od.order.status = :status
            """)
    Long calculateSold(@Param("id") Integer id, OrderStatus status);
    @Query("""
    SELECT COALESCE(SUM(od.quantity), 0)
    FROM OrderDetail od
    WHERE od.product.id = :id
      AND od.order.status = :status
      AND od.order.createdAt >= :from
      AND od.order.createdAt < :to
""")
    Long calculateSoldByDate(
            @Param("id") Integer id,
            @Param("status") OrderStatus status,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );
}
