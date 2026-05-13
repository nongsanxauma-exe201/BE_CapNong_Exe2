package com.swd301.foodmarket.repository;

import com.swd301.foodmarket.entity.OrderDetail;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderDetailRepository extends JpaRepository<OrderDetail,Integer>
{
}
