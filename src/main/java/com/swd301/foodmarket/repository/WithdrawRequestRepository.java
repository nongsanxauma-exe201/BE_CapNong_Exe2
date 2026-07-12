package com.swd301.foodmarket.repository;

import com.swd301.foodmarket.entity.WithdrawRequest;
import com.swd301.foodmarket.enums.WithdrawRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface WithdrawRequestRepository extends JpaRepository<WithdrawRequest, Integer> {

    List<WithdrawRequest> findByWallet_ShopOwner_Id(Integer shopOwnerId);

    List<WithdrawRequest> findByWallet_Shipper_IdOrderByIdDesc(Integer shipperId);

    List<WithdrawRequest> findByStatus(WithdrawRequestStatus status);

    @Query("""
            SELECT wr FROM WithdrawRequest wr
            JOIN FETCH wr.wallet w
            LEFT JOIN FETCH w.shopOwner
            LEFT JOIN FETCH w.shipper
            LEFT JOIN FETCH w.buyer
            WHERE wr.status = :status
            ORDER BY wr.id DESC
            """)
    List<WithdrawRequest> findByStatusWithWallet(@Param("status") WithdrawRequestStatus status);

    List<WithdrawRequest> findByWallet_ShopOwner_IdOrderByIdDesc(Integer shopOwnerId);

    List<WithdrawRequest> findAllByOrderByIdDesc();

    @Query("""
            SELECT wr FROM WithdrawRequest wr
            JOIN FETCH wr.wallet w
            LEFT JOIN FETCH w.shopOwner
            LEFT JOIN FETCH w.shipper
            LEFT JOIN FETCH w.buyer
            ORDER BY wr.id DESC
            """)
    List<WithdrawRequest> findAllWithWalletOrderByIdDesc();

    List<WithdrawRequest> findByWallet_Buyer_IdOrderByIdDesc(Integer walletBuyerId);
}
