package com.swd301.foodmarket.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
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
    @Builder.Default
    private boolean refunded = false;
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
