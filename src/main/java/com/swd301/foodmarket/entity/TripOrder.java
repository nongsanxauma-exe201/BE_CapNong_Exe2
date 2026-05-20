//package com.swd301.foodmarket.entity;
//
//import com.fasterxml.jackson.annotation.JsonIgnore;
//import com.swd301.foodmarket.enums.JoinedType;
//import com.swd301.foodmarket.enums.TripOrderStatus;
//import jakarta.persistence.*;
//import lombok.*;
//import lombok.experimental.FieldDefaults;
//import java.time.LocalDateTime;
//
//@Getter
//@Setter
//@NoArgsConstructor
//@AllArgsConstructor
//@Builder
//@FieldDefaults(level = AccessLevel.PRIVATE)
//@Entity
//@Table(name = "trip_orders")
//public class TripOrder {
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    Integer id;
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "trip_id", nullable = false)
//
//    Trip trip;
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "order_id", nullable = false)
//
//    Order order;
//
//    @Column(name = "joined_type", nullable = false)
//    @Enumerated(EnumType.STRING)
//    JoinedType joinedType; // OWNER or SHARED
//
//    @Column(name = "pickup_sequence")
//    Integer pickupSequence;
//
//    @Column(name = "weight_kg")
//    Double weightKg;
//
//    @Column(name = "estimated_pickup_time")
//    LocalDateTime estimatedPickupTime;
//
//    @Column(name = "estimated_arrival_warehouse_time")
//    LocalDateTime estimatedArrivalWarehouseTime;
//
//    @Column(name = "status", nullable = false)
//    @Enumerated(EnumType.STRING)
//    TripOrderStatus status;
//}
