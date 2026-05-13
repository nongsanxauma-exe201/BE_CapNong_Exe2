package com.swd301.foodmarket.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.swd301.foodmarket.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
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

    @ManyToOne
    @JoinColumn(name = "shipper_id")
    @JsonIgnore
    User shipper;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    List<OrderDetail> orderDetails;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    List<OrderMysteryBox> mysteryBoxes;

    @OneToMany(mappedBy = "order")
    @JsonIgnore
    List<BuildCombo> buildCombos;

    @OneToMany(mappedBy = "order")
    @JsonIgnore
    List<Payment> payments;

    // ================= DATA =================

    @Column(name = "shipping_fee", precision = 18, scale = 2)
    BigDecimal shippingFee;

    @Column(name = "recipient_name", length = 50)
    String recipientName;

    @Column(name = "recipient_phone", length = 20)
    String recipientPhone;

    @Column(name = "shipping_address", length = 255)
    String shippingAddress;

    // Cache tọa độ nhà buyer (điểm giao hàng)
    @Column(name = "shipping_latitude")
    Double shippingLatitude;

    @Column(name = "shipping_longitude")
    Double shippingLongitude;

    // ✅ MỚI: Cache tọa độ shop (điểm lấy hàng) - tránh gọi Goong API lặp lại
    @Column(name = "shop_latitude")
    Double shopLatitude;

    @Column(name = "shop_longitude")
    Double shopLongitude;

    @Column(name = "estimated_delivery_date")
    LocalDateTime estimatedDeliveryDate;

    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;

    @Column(length = 500)
    String note;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    OrderStatus status;


    @Column(precision = 18, scale = 2)
    BigDecimal totalAmount;

    @Column(length = 50)
    String paymentMethod;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }
}