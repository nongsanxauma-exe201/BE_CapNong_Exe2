package com.swd301.foodmarket.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.swd301.foodmarket.enums.UserStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id;

    // ===== AUTH =====
    @Column(unique = true, nullable = false)
    String email;

    @Column(nullable = true)
    String password;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id")
    Role role;

    @Enumerated(EnumType.STRING)
    UserStatus status;

    // ===== PROFILE =====
    String fullName;
    String phoneNumber;
    String address;

    // ===== SHOP INFO =====
    String shopName;
    // Thông tin ngân hàng
    String bankName;
    String bankAccount;
    String bankAccountHolder;

    Double ratingAverage;

    @Column(length = 500)
    String description;

    String logoUrl;
    String achievement;

    // ===== SHIPPER =====
    String license;
    String vehicleNumber;
    String licenseImageUrl;
    String vehicleDocImageUrl;

    // ================= RELATIONSHIPS =================

    // ADMIN
    @OneToMany(mappedBy = "admin")
    @JsonIgnore
    List<Blog> blogs;

    @OneToMany(mappedBy = "admin")
    @JsonIgnore
    List<Notification> adminNotifications;

    @OneToMany(mappedBy = "admin")
    @JsonIgnore
    List<WithdrawRequest> withdrawRequests;

    @OneToOne(mappedBy = "admin")
    @JsonIgnore
    Wallet adminWallet;

    // SHOP OWNER
    @OneToOne(mappedBy = "shopOwner")
    @JsonIgnore
    Wallet shopWallet;

    @OneToMany(mappedBy = "shopOwner")
    @JsonIgnore
    List<Product> products;

    @OneToMany(mappedBy = "shopOwner")
    @JsonIgnore
    List<Voucher> vouchers;

    @OneToMany(mappedBy = "shopOwner")
    @JsonIgnore
    List<Order> shopOrders;

    @OneToMany(mappedBy = "shopOwner")
    @JsonIgnore
    List<Review> shopReviews;

    @OneToMany(mappedBy = "shopOwner")
    @JsonIgnore
    List<ReturnRequest> shopReturns;

    @OneToMany(mappedBy = "shopOwner")
    @JsonIgnore
    List<MysteryBox> mysteryBoxes;

    @OneToMany(mappedBy = "shopOwner")
    @JsonIgnore
    List<BuildCombo> combos;

    // BUYER
    @OneToMany(mappedBy = "buyer")
    @JsonIgnore
    List<Order> buyerOrders;

    @OneToMany(mappedBy = "buyer")
    @JsonIgnore
    List<Review> buyerReviews;

    @OneToMany(mappedBy = "buyer")
    @JsonIgnore
    List<ReturnRequest> buyerReturns;

    @OneToMany(mappedBy = "buyer")
    @JsonIgnore
    List<BuildPlan> buildPlans;

    @OneToOne(mappedBy = "buyer")
    @JsonIgnore
    Wallet buyerWallet;

    // Per-user notification states (via mapping table)
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    List<UserNotification> userNotifications;

    // SHIPPER
    @OneToMany(mappedBy = "shipper")
    @JsonIgnore
    List<Order> shipperOrders;

    LocalDateTime lockedAt;

    LocalDateTime createAt;
}
