package com.swd301.foodmarket.repository;

import com.swd301.foodmarket.entity.ShipperLocation;
import com.swd301.foodmarket.entity.User;
import com.swd301.foodmarket.enums.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShipperLocationRepository extends JpaRepository<ShipperLocation, Integer> {

    // Lấy vị trí hiện tại của shipper (dùng cho REST fallback)
    Optional<ShipperLocation> findByShipper(User shipper);

    // Lấy vị trí shipper đang giao 1 đơn cụ thể
    Optional<ShipperLocation> findByCurrentOrder_Id(Integer orderId);

    // Lấy tất cả shipper đang online (có bản ghi location)
    List<ShipperLocation> findAll();

    List<ShipperLocation> findByShipper_Status(UserStatus status);
}