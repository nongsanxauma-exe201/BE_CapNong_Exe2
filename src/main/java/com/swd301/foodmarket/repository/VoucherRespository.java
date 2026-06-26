package com.swd301.foodmarket.repository;

import com.swd301.foodmarket.entity.User;
import com.swd301.foodmarket.entity.Voucher;
import com.swd301.foodmarket.enums.VoucherType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Optional;


public interface VoucherRespository extends JpaRepository<Voucher, Integer> {

    Voucher findByVoucherCode(String voucherCode);
    Optional<Voucher> findByVoucherTypeAndAutoCreatedFromOrderId(VoucherType voucherType, Integer autoCreatedFromOrderId);

    Page<Voucher> findByShopOwnerAndActive(User shopOwner, boolean active, Pageable pageable);

    Page<Voucher> findByVoucherTypeAndActive(VoucherType voucherType, boolean active, Pageable pageable);

@Query("""
select v from Voucher v
where (v.expiryDate is null or v.expiryDate >= :now)
  and v.active = true
  and (v.claimLimit is null or v.claimedCount < v.claimLimit)
  and v.shopOwner = :shopOwner
  and v.voucherType = com.swd301.foodmarket.enums.VoucherType.SHOP
"""
)
    Page<Voucher>  getVoucherForBuyer(User shopOwner, LocalDateTime now,  Pageable pageable);
}
