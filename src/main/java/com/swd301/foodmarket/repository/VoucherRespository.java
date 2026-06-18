package com.swd301.foodmarket.repository;

import com.swd301.foodmarket.entity.User;
import com.swd301.foodmarket.entity.Voucher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;


public interface VoucherRespository extends JpaRepository<Voucher, Integer> {

    Voucher findByVoucherCode(String voucherCode);

    Page<Voucher> findByShopOwnerAndActive(User shopOwner, boolean active, Pageable pageable);

@Query("""
select  v from Voucher v where  v.expiryDate >= :now and v.active = true and v.claimedCount  < v.claimLimit
and v.shopOwner = :shopOwner
"""
)
    Page<Voucher>  getVoucherForBuyer(User shopOwner, LocalDateTime now,  Pageable pageable);
}
