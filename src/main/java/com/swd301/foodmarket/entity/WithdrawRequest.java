package com.swd301.foodmarket.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.swd301.foodmarket.enums.PayoutStatus;
import com.swd301.foodmarket.enums.WithdrawRequestStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
@Entity
@Table(name = "withdraw_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WithdrawRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "request_id")
    Integer id;

    // ================= RELATIONSHIPS =================

    @ManyToOne
    @JoinColumn(name = "admin_id")
    @JsonIgnore
    User admin;

    @ManyToOne
    @JoinColumn(name = "wallet_id", nullable = false)
    @JsonIgnore
    Wallet wallet;

    // ================= DATA =================

    @Column(length = 255)
    String reason;

    @Column(length = 255)
    String adminNote;

    @CreationTimestamp
    LocalDateTime createdAt;

    LocalDateTime processedAt;

    LocalDateTime payoutAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    WithdrawRequestStatus status = WithdrawRequestStatus.PENDING;
    // REQUESTED, APPROVED, PROCESSING, SUCCESS, FAILED

    @Column(nullable = false, precision = 18, scale = 2)
    BigDecimal amount;

    @Column(nullable = true, precision = 18, scale = 2)
    BigDecimal fee;

    @Column(nullable = true, precision = 18, scale = 2)
    BigDecimal receiveAmount;

    @Column(length = 100)
    String bankName;

    @Column(length = 50)
    String bankAccountNumber;

    @Column(length = 100)
    String bankAccountHolder;

    // ========== PayOS / payout info ==========

    @Column(length = 50)
    String payoutProvider; // PAYOS

    @Column(length = 100, unique = true)
    String payoutOrderCode; // WITHDRAW_123

    @Column(length = 100)
    String payoutTransactionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    PayoutStatus payoutStatus = PayoutStatus.CREATED;
    // CREATED, PROCESSING, PAID, FAILED

    @Column(length = 255)
    String payoutMessage;

    @Column(name = "payout_qr_url", columnDefinition = "TEXT")
    private String payoutQrUrl;

    @Column(name = "payout_checkout_url", columnDefinition = "TEXT")
    private String payoutCheckoutUrl;
}