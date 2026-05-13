package com.swd301.foodmarket.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.swd301.foodmarket.enums.WalletStatus;
import com.swd301.foodmarket.enums.WalletType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "wallets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "wallet_id")
    Integer id;

    // ===============================
    // SHOP OWNER WALLET
    // ===============================
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id", unique = true)
    @JsonIgnore
    User shopOwner;

    // ===============================
    // SHIPPER WALLET
    // ===============================
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipper_id", unique = true)
    @JsonIgnore
    User shipper;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id", unique = true)
    @JsonIgnore
    User admin;

    // ===============================
    // BUYER WALLET
    // ===============================
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", unique = true)
    @JsonIgnore
    User buyer;

    // ===============================
    // WITHDRAW REQUESTS
    // ===============================
    @OneToMany(mappedBy = "wallet",
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    @JsonIgnore
    List<WithdrawRequest> withdrawRequests;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    WalletStatus status;

    @Column(name = "create_at")
    LocalDateTime createAt;

    @Column(name = "total_balance", precision = 18, scale = 2)
    BigDecimal totalBalance;

    @Column(name = "frozen_balance", precision = 18, scale = 2)
    BigDecimal frozenBalance;

    @Column(name = "total_revenue_all_time", precision = 18, scale = 2)
    BigDecimal totalRevenueAllTime;

    @Column(name = "total_withdrawn", precision = 18, scale = 2)
    BigDecimal totalWithdrawn;

    @Column(name = "commission_wallet", precision = 18, scale = 2)
    BigDecimal commissionWallet;

    @Column(name = "updated_at")
    LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    WalletType type;

    @PrePersist
    void onCreate() {
        createAt = LocalDateTime.now();

        if (totalBalance == null) totalBalance = BigDecimal.ZERO;
        if (frozenBalance == null) frozenBalance = BigDecimal.ZERO;
        if (totalRevenueAllTime == null) totalRevenueAllTime = BigDecimal.ZERO;
        if (totalWithdrawn == null) totalWithdrawn = BigDecimal.ZERO;
        if (commissionWallet == null) commissionWallet = BigDecimal.ZERO;
    }
}
