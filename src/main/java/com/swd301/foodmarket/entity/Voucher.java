package com.swd301.foodmarket.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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

    @Column(name = "usage_limit")
    Integer usageLimit;

    @Column(name = "expiry_date")
    LocalDateTime expiryDate;
}
