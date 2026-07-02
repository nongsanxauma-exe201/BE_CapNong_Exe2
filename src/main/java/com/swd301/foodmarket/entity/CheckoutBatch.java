package com.swd301.foodmarket.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.swd301.foodmarket.enums.CheckoutBatchStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "checkout_batches")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CheckoutBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id;

    @ManyToOne
    @JoinColumn(name = "buyer_id", nullable = false)
    @JsonIgnore
    User buyer;

    @Column(name = "total_amount", precision = 18, scale = 2)
    BigDecimal totalAmount;

    @Column(name = "payment_method", length = 20)
    String paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    CheckoutBatchStatus status;

    @Column(name = "payos_order_code", unique = true)
    String payosOrderCode;

    @Column(name = "checkout_url", columnDefinition = "TEXT")
    String checkoutUrl;

    @Column(name = "qr_code_url", columnDefinition = "TEXT")
    String qrCodeUrl;

    @Column(name = "batch_voucher_user_id")
    UUID batchVoucherUserId;

    @Column(name = "batch_discount_amount", precision = 18, scale = 2)
    BigDecimal batchDiscountAmount;

    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;

    @Column(name = "updated_at")
    LocalDateTime updatedAt;

    @OneToMany(mappedBy = "checkoutBatch")
    @JsonIgnore
    @Builder.Default
    List<Order> orders = new ArrayList<>();

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
    }
}
