package com.swd301.foodmarket.repository;

import com.swd301.foodmarket.entity.User;
import com.swd301.foodmarket.entity.Wallet;
import com.swd301.foodmarket.entity.WithdrawRequest;
import com.swd301.foodmarket.enums.WalletType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface WalletRepository extends JpaRepository<Wallet, Integer> {

    Optional<Wallet> findByShopOwner_Id(Integer shopOwnerId);

    Optional<Wallet> findByShopOwner(User shopOwner);

    @Query("SELECT w FROM Wallet w WHERE w.shopOwner.id = ?1 OR w.admin.id = ?1 OR w.shipper.id = ?1 OR w.buyer.id = ?1")
    Optional<Wallet> findByUserId(Integer userId);

    Optional<Wallet> findByAdmin_Id(Integer adminId);

    boolean existsByShopOwner_Id(Integer shopOwnerId);

    boolean existsByAdmin_Id(Integer adminId);

    boolean existsByShopOwner(User shopOwner);

    Optional<Wallet> findByType(WalletType type);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Wallet w WHERE w.type = :type")
    Optional<Wallet> findByTypeForUpdate(WalletType type);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Wallet w WHERE w.shopOwner.id = :shopId")
    Optional<Wallet> findByShopOwnerIdForUpdate(Integer shopId);

    boolean existsByShipper(User user);

    Optional<Wallet> findByShipper(User user);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Wallet w WHERE w.shipper.id = :shipperId")
    Optional<Wallet> findByShipperIdForUpdate(Integer shipperId);

}