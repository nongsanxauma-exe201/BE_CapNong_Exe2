package com.swd301.foodmarket.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "order_mystery_boxes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderMysteryBox {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "order_id")
    private Order order;

    @ManyToOne
    @JoinColumn(name = "mystery_box_id")
    private MysteryBox mysteryBox;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "unit_weight_kg", precision = 10, scale = 2)
    private BigDecimal unitWeightKg;

    @Column(name = "rescued_weight_kg", precision = 10, scale = 2)
    private BigDecimal rescuedWeightKg;

    @OneToOne(mappedBy = "orderMysteryBox")
    private QualityResult qualityResult;
}
