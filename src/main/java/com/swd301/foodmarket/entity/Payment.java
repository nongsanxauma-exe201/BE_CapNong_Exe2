package com.swd301.foodmarket.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.swd301.foodmarket.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    Integer id;

    // ================= RELATIONSHIPS =================

    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)
    @JsonIgnore
    Order order;

    @ManyToOne
    @JoinColumn(name = "withdraw_request_id")
    @JsonIgnore
    WithdrawRequest withdrawRequest;

    @OneToMany(mappedBy = "payment")
    @JsonIgnore
    List<Transaction> transactions;

    // ================= DATA =================

    LocalDateTime paymentDate;

    @Column(precision = 18, scale = 2)
    BigDecimal amount;

    @Column(length = 50)
    String paymentGateway;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    PaymentStatus status;

    @Column(updatable = false)
    LocalDateTime createdAt;

    LocalDateTime updatedAt;

    @Column(name = "payos_order_code", unique = true)
    String payosOrderCode;

    @Column(columnDefinition = "TEXT")
    String checkoutUrl;

    @Column(columnDefinition = "TEXT")
    String qrCodeUrl;
}
