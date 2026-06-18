package com.swd301.foodmarket.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
@Table(name = "voucher_users")
public class VoucherUser {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne
    @JoinColumn(name = "voucher_id", nullable = false)
    @OrderBy("expiryDate ASC")
    private Voucher voucher;
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @Builder.Default
    private Integer quantityUsed = 0 ;
    @Builder.Default
    private Integer quantity =0 ;
    @Builder.Default
    private LocalDateTime claimAt = LocalDateTime.now();
}
