package com.swd301.foodmarket.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Entity
@Table(name = "shipper_locations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ShipperLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id;

    // Shipper nào đang gửi vị trí
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipper_id", nullable = false, unique = true)
    User shipper;

    // Đơn hàng đang giao (nullable - shipper có thể online nhưng chưa nhận đơn)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    Order currentOrder;

    @Column(nullable = false)
    Double latitude;

    @Column(nullable = false)
    Double longitude;

    LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}