package com.swd301.foodmarket.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Entity
@Table(name = "reviews")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_id")
    Integer id;

    // ================= RELATIONSHIPS =================

    @ManyToOne
    @JoinColumn(name = "buyer_id", nullable = false)
    @JsonIgnore
    User buyer;

    @ManyToOne
    @JoinColumn(name = "shop_owner_id", nullable = false)
    @JsonIgnore
    User shopOwner;

    @OneToOne
    @JoinColumn(name = "order_detail_id", nullable = false, unique = true)
    @JsonIgnore
    OrderDetail orderDetail;

    @ManyToOne
    @JoinColumn(name = "mystery_box_id")
    MysteryBox mysteryBox;

    // ================= DATA =================

    @Column(precision = 5, scale = 2)
    BigDecimal ratingStar;

    @Column(length = 255)
    String comment;

    @Column(length = 500)
    String evidence;

    @Column(length = 255)
    String replyFromShop;
}
