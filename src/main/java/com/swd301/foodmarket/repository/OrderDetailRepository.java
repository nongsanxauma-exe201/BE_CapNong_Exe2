package com.swd301.foodmarket.repository;

import com.swd301.foodmarket.entity.OrderDetail;
import com.swd301.foodmarket.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderDetailRepository extends JpaRepository<OrderDetail,Integer>
{
    Integer countByOrder_Status(OrderStatus orderStatus);
}
