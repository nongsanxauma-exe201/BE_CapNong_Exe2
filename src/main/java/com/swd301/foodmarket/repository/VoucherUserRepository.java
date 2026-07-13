package com.swd301.foodmarket.repository;

import com.swd301.foodmarket.entity.User;
import com.swd301.foodmarket.entity.Voucher;
import com.swd301.foodmarket.entity.VoucherUser;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface VoucherUserRepository extends JpaRepository<VoucherUser, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT vu FROM VoucherUser vu WHERE vu.id = :id")
    Optional<VoucherUser> findByIdForUpdate(@Param("id") UUID id);

    java.util.Optional<VoucherUser> findByIdAndUser(UUID id, User user);
    java.util.Optional<VoucherUser> findByUserAndVoucher_VoucherCode(User user, String voucherCode);
    java.util.Optional<VoucherUser> findByUserAndVoucher(User user, Voucher voucher);

    Page<VoucherUser> findByUserAndVoucher_ExpiryDateAfterAndVoucher_Active(User user, LocalDateTime voucherExpiryDateAfter, boolean voucherActive, Pageable pageable);

    boolean existsByUserAndVoucher(User user, Voucher voucher);

    Page<VoucherUser> findByUserAndVoucher_ExpiryDateAfterAndVoucher_ActiveAndVoucher_ShopOwnerAndVoucher_MinOrderValueLessThanEqual(User user, LocalDateTime voucherExpiryDateAfter, boolean voucherActive, User voucherShopOwner, BigDecimal voucherMinOrderValueIsLessThan, Pageable pageable);

    @Query("""
    select vu
    from VoucherUser vu
    join vu.voucher v
    where vu.user = :user
      and (v.expiryDate is null or v.expiryDate > :voucherExpiryDateAfter)
      and v.active = :voucherActive
      and vu.quantity > 0
      and (v.usageLimit is null or v.usedCount < v.usageLimit)
""")
    Page<VoucherUser> getMyVoucherOfBuyerCanUse(
            @Param("user") User user,
            @Param("voucherExpiryDateAfter") LocalDateTime voucherExpiryDateAfter,
            @Param("voucherActive") boolean voucherActive,
            Pageable pageable
    );

    @Query("""
    select vu
    from VoucherUser vu
    join vu.voucher v
    where vu.user = :user
      and (
          (v.shopOwner = :shop and v.voucherType = com.swd301.foodmarket.enums.VoucherType.SHOP)
          or (v.voucherType = com.swd301.foodmarket.enums.VoucherType.DAMAGED_ORDER and v.shopOwner = :shop)
      )
      and (v.expiryDate is null or v.expiryDate > :voucherExpiryDateAfter)
      and v.active = :voucherActive
      and vu.quantity > 0
      and (v.usageLimit is null or v.usedCount < v.usageLimit)
      and (v.minOrderValue is null or v.minOrderValue <= :amount)
""")
    Page<VoucherUser> getMyVoucherForOrder(
            @Param("user") User user,
            @Param("voucherExpiryDateAfter") LocalDateTime voucherExpiryDateAfter,
            @Param("voucherActive") boolean voucherActive,
            @Param("amount") BigDecimal amount,
            @Param("shop") User shop,
            Pageable pageable
    );

    @Query("""
    select vu
    from VoucherUser vu
    join vu.voucher v
    where vu.user = :user
      and v.voucherType in (
          com.swd301.foodmarket.enums.VoucherType.PLATFORM,
          com.swd301.foodmarket.enums.VoucherType.WELCOME
      )
      and (v.expiryDate is null or v.expiryDate > :voucherExpiryDateAfter)
      and v.active = :voucherActive
      and vu.quantity > 0
      and (v.usageLimit is null or v.usedCount < v.usageLimit)
      and (v.minOrderValue is null or v.minOrderValue <= :amount)
""")
    Page<VoucherUser> getMyBatchVouchers(
            @Param("user") User user,
            @Param("voucherExpiryDateAfter") LocalDateTime voucherExpiryDateAfter,
            @Param("voucherActive") boolean voucherActive,
            @Param("amount") BigDecimal amount,
            Pageable pageable
    );
}
