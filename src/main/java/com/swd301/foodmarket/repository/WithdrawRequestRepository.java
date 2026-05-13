package com.swd301.foodmarket.repository;

import com.swd301.foodmarket.entity.WithdrawRequest;
import com.swd301.foodmarket.enums.WithdrawRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WithdrawRequestRepository extends JpaRepository<WithdrawRequest, Integer> {

    List<WithdrawRequest> findByWallet_ShopOwner_Id(Integer shopOwnerId);

    List<WithdrawRequest> findByWallet_Shipper_IdOrderByIdDesc(Integer shipperId);

    List<WithdrawRequest> findByStatus(WithdrawRequestStatus status);

    List<WithdrawRequest> findByWallet_ShopOwner_IdOrderByIdDesc(Integer shopOwnerId);

    List<WithdrawRequest> findAllByOrderByIdDesc();
}
