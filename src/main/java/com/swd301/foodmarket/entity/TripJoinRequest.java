package com.swd301.foodmarket.entity;


import com.swd301.foodmarket.enums.TripJoinStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "trip_join_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TripJoinRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
 
    Trip trip;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "farmer_id", nullable = false)
 
    User farmer;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "trip_join_request_orders",
            joinColumns = @JoinColumn(name = "trip_join_request_id"),
            inverseJoinColumns = @JoinColumn(name = "order_id")
    )
    @Builder.Default
    List<Order> orders = new ArrayList<>();

    @Column(name = "requested_kg")
    Double requestedKg;

    @Column(name = "pickup_lat")
    Double pickupLat;

    @Column(name = "pickup_lng")
    Double pickupLng;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    TripJoinStatus status;

    @Column(name = "price_offer", precision = 18, scale = 2)
    BigDecimal priceOffer;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));

}
