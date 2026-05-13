package com.swd301.foodmarket.entity;

import jakarta.persistence.*;
import lombok.*;

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
}
