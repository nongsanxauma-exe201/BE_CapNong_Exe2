package com.swd301.foodmarket.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "mystery_boxes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MysteryBox {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "mystery_id")
    Integer id;

    // ================= RELATIONSHIPS =================

    // N Box → 1 Shop Owner
    @ManyToOne
    @JoinColumn(name = "shop_owner_id")
    @JsonIgnore
    User shopOwner;



    // 1 Box → N ProductMystery
    @OneToMany(mappedBy = "mysteryBox",
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    private List<ProductMystery> productMysteries;

    // ================= DATA =================

    @Column(name = "box_type", length = 50)
    String boxType;

    @Column(name = "image_url", length = 255)
    String imageUrl;

    @Column(name = "price", precision = 18, scale = 2)
    BigDecimal price;

    @Column(length = 500)
    String description;

    @Column(length = 255)
    String note;

    @Column(name = "is_active")
    @Builder.Default
    Boolean isActive = true;

    @Column(name = "total_quantity")
    Integer totalQuantity;

    @Column(name = "sold_quantity")
    @Builder.Default
    Integer soldQuantity = 0;
}
