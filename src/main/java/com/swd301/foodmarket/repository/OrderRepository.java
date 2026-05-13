package com.swd301.foodmarket.repository;

import com.swd301.foodmarket.entity.Order;
import com.swd301.foodmarket.entity.User;
import com.swd301.foodmarket.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order,Integer>
{

    List<Order> findByShipperIsNullAndStatus(OrderStatus status);

    // Đơn của shipper này
    List<Order> findByShipper(User shipper);
    List<Order> findByBuyerId(Integer buyerId);

    long countByBuyerIdAndStatus(Integer buyerId, OrderStatus status);

    Page<Order> findAll(Pageable pageable);
    Page<Order> findByBuyerId(Integer buyerId, Pageable pageable);

    // Đơn của shipper theo trạng thái
    List<Order> findByShipperAndStatus(User shipper, OrderStatus status);

    /**
     * Lấy tất cả đơn hàng giữa buyer và shopOwner (cả 2 chiều).
     * Dùng LEFT JOIN FETCH để load sẵn items, tránh N+1 query.
     *
     * Lưu ý: do có nhiều LEFT JOIN FETCH nên dùng DISTINCT
     * để tránh Hibernate trả về duplicate rows.
     */
    @Query("""
    SELECT DISTINCT o FROM Order o
    WHERE (o.buyer = :userA AND o.shopOwner = :userB)
       OR (o.buyer = :userB AND o.shopOwner = :userA)
    ORDER BY o.createdAt DESC
""")
    List<Order> findOrdersBetweenUsers(@Param("userA") User userA,
                                       @Param("userB") User userB);


}
