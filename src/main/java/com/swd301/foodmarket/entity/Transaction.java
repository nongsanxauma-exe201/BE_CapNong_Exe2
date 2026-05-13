package com.swd301.foodmarket.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.swd301.foodmarket.enums.TransactionStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Entity
@Table(name = "transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_id")
    Integer id;

    // ================= RELATIONSHIPS =================

    @ManyToOne
    @JoinColumn(name = "payment_id")
    @JsonIgnore
    Payment payment;

    // ================= DATA =================

    @Column(length = 50)
    String paymentGateway;

    @Column(precision = 18, scale = 2)
    BigDecimal amount;

    @Column(precision = 18, scale = 2)
    BigDecimal balanceAfter;

    @Enumerated(EnumType.STRING)
    TransactionStatus status;

    @Column(length = 500)
    String content;
}
