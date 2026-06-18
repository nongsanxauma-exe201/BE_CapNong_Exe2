package com.swd301.foodmarket.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
@Table(name = "voucher_usage")
public class VoucherUsage {

        @Id
        @GeneratedValue(strategy = GenerationType.UUID)
        private UUID id;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "voucher_id", nullable = false)
        private Voucher voucher;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "user_id", nullable = false)
        private User user;

        @OneToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "order_id", nullable = false, unique = true)
        private Order order;

        private BigDecimal discountAmount;
       @Builder.Default
        private LocalDateTime usedAt = LocalDateTime.now();

}
