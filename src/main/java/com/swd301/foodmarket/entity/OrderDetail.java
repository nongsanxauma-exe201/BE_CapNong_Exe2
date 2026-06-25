package com.swd301.foodmarket.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "order_details")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_detail_id")
    Integer id;

    // ================= RELATIONSHIPS =================

    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)
    @JsonIgnore
    Order order;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    @JsonIgnore
    Product product;

    // Return requests
    @OneToMany(mappedBy = "orderDetail")
    @JsonIgnore
    List<ReturnRequest> returnRequests;

    // Review
    @OneToOne(mappedBy = "orderDetail")
    @JsonIgnore
    Review review;

    // ================= DATA =================

    Integer quantity;

    @Column(name = "quantity_kg", precision = 12, scale = 2)
    BigDecimal quantityKg;

    @Column(precision = 18, scale = 2)
    BigDecimal unitPrice;

    @Column(name = "price_per_kg", precision = 18, scale = 2)
    BigDecimal pricePerKg;

    @Column(name = "unit_weight_kg", precision = 10, scale = 2)
    BigDecimal unitWeightKg;

    @Column(name = "rescued_weight_kg", precision = 10, scale = 2)
    BigDecimal rescuedWeightKg;

    @Column(name = "is_requested_return", nullable = false)
    @Builder.Default
    Boolean isRequestedReturn = false;
    @OneToOne(mappedBy = "orderDetail")
    private QualityResult qualityResult;
}
