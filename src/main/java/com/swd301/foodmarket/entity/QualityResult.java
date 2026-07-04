package com.swd301.foodmarket.entity;

import jakarta.persistence.*;
import lombok.*;

import com.swd301.foodmarket.enums.QcDamageTier;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "quality_result")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QualityResult {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID qualityResultId;
    @Column(columnDefinition = "TEXT")
    private String note;
    @Column(name = "damage_note", length = 500)
    private String damageNote;
    @Builder.Default
    private boolean autoVoucherCreated = false;
    @Enumerated(EnumType.STRING)
    private com.swd301.foodmarket.enums.QualityResult qualityResult;
    @OneToOne
    @JoinColumn(name = "order_detail_id")
    private OrderDetail orderDetail;
    @OneToOne
    @JoinColumn(name = "order_mystery_box_id")
    private OrderMysteryBox orderMysteryBox;
    @OneToMany(mappedBy = "qualityResult")
    private List<QualityCheck> qualityChecks = new ArrayList<>();
    private Double  rejectedWeight = 0.0;
    @Column(name = "requested_kg", precision = 12, scale = 2)
    private BigDecimal requestedKg;
    @Column(name = "approved_kg", precision = 12, scale = 2)
    private BigDecimal approvedKg;
    @Column(name = "rejected_kg", precision = 12, scale = 2)
    private BigDecimal rejectedKg;
    @Column(name = "refunded_kg", precision = 12, scale = 2)
    private BigDecimal refundedKg;
    @Enumerated(EnumType.STRING)
    @Column(name = "damage_tier", length = 20)
    private QcDamageTier damageTier;
    @Builder.Default
    @Column(name = "line_cancelled")
    private boolean lineCancelled = false;
    @Column(name = "line_refund_amount", precision = 18, scale = 2)
    private BigDecimal lineRefundAmount;
    @Builder.Default
    private boolean refunded = false;
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
}
