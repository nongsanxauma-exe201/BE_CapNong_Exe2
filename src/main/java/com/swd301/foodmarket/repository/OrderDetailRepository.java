package com.swd301.foodmarket.repository;

import com.swd301.foodmarket.entity.Order;
import com.swd301.foodmarket.entity.OrderDetail;
import com.swd301.foodmarket.entity.QualityResult;
import com.swd301.foodmarket.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OrderDetailRepository extends JpaRepository<OrderDetail,Integer>
{
    Integer countByOrder_Status(OrderStatus orderStatus);

@Query("""
select  count(od) from OrderDetail od
where od.order = :order and od.qualityResult is not null

""")
    Integer countByOrderAndQualityResultNull(Order order);
}
