package com.swd301.foodmarket.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CartItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    private Cart cart;

    @ManyToOne
    private Product product;

    @ManyToOne
    @JoinColumn(name = "mystery_box_id")
    private MysteryBox mysteryBox;

    @ManyToOne
    @JoinColumn(name = "build_combo_id")
    private BuildCombo buildCombo;  // null nếu là product/mysteryBox


    private Integer quantity;
}
