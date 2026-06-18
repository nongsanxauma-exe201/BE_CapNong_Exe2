package com.swd301.foodmarket.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "vouchers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Voucher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "voucher_id")
    Integer id;

    // ===============================
    // N-1 → Shop Owner (User)
    // ===============================
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id", nullable = false)
    @JsonIgnore
    User shopOwner;

    @Column(name = "voucher_code", length = 20, nullable = false, unique = true)
    String voucherCode;

    @Column(name = "discount_value", precision = 18, scale = 2)
    BigDecimal discountValue;

    @Column(name = "max_discount", precision = 18, scale = 2)
    BigDecimal maxDiscount;

    @Column(name = "min_order_value", precision = 18, scale = 2)
    BigDecimal minOrderValue;

    private Integer claimLimit;
    @Builder.Default// tổng số lượt được nhận
    private Integer claimedCount = 0;      // đã có bao nhiêu lượt nhận

    private Integer usageLimit;
    @Builder.Default// tổng số lượt được dùng
    private Integer usedCount =0 ;         // đã dùng bao nhiêu lượt

    private Integer perUserLimit;      // mỗi user dùng được mấy lần
    @Column(name = "expiry_date")
    LocalDateTime expiryDate;
    @Builder.Default
    private boolean active = true;

    @Builder.Default
    private LocalDateTime createAt = LocalDateTime.now();
    @OneToMany(mappedBy = "voucher", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<VoucherUser> voucherUsers = new ArrayList<>();
}
