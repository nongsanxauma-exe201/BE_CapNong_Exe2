package com.swd301.foodmarket.repository;

import com.swd301.foodmarket.entity.ReturnRequest;
import com.swd301.foodmarket.enums.ReturnStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReturnRequestRepository extends JpaRepository<ReturnRequest, Integer> {
    List<ReturnRequest> findByBuyerId(Integer buyerId);
    List<ReturnRequest> findByShopOwnerId(Integer shopOwnerId);
    List<ReturnRequest> findByStatus(ReturnStatus status);
    java.util.Optional<ReturnRequest> findByPayoutOrderCode(Long payoutOrderCode);
}
