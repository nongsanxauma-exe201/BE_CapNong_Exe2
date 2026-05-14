package com.swd301.foodmarket.entity;

import jakarta.persistence.*;
import lombok.*;

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
    private String qualityResultId;
    @Column(columnDefinition = "TEXT")
    private String note;
    @Enumerated(EnumType.STRING)
    private com.swd301.foodmarket.enums.QualityResult qualityResult;
    @OneToOne
    @JoinColumn(name = "order_detail_id")
    private OrderDetail orderDetail;
}
