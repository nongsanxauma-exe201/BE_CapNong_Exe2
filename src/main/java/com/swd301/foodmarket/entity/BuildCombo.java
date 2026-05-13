package com.swd301.foodmarket.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.swd301.foodmarket.enums.MealType;
import com.swd301.foodmarket.enums.Region;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "build_combos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BuildCombo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "combo_id")
    Integer id;

    // ================= RELATIONSHIPS =================

    // N Combo → 1 Shop Owner
    @ManyToOne
    @JoinColumn(name = "shop_owner_id")
    @JsonIgnore
    User shopOwner;

    // N Combo → 1 Order
    @ManyToOne
    @JoinColumn(name = "order_id")
    @JsonIgnore
    Order order;

    // M-N Products
    @OneToMany(mappedBy = "combo", cascade = CascadeType.ALL, orphanRemoval = true)
    List<ProductCombo> items;

    // ================= DATA =================

    @Column(name = "combo_name", length = 255)
    String comboName;

    @Column(name = "discount_price", precision = 18, scale = 2)
    BigDecimal discountPrice;

    @Column(length = 500)
    String description;

    @Column(length = 255)
    String type; // Avaiable / Custom

    @Enumerated(EnumType.STRING)
    @Column(name = "region")
    Region region;

    @Enumerated(EnumType.STRING)
    @Column(name = "meal_type")
    MealType mealType;

    @Column(name = "image_url", length = 500)
    String imageUrl;
}
