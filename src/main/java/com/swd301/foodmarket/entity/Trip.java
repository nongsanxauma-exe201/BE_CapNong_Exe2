package com.swd301.foodmarket.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.swd301.foodmarket.enums.TripStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "trips")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Trip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
 
    User owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id")
 
    Vehicle vehicle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id")
 
    Warehouse warehouse;

    @Column(name = "start_lat")
    Double startLat;

    @Column(name = "start_lng")
    Double startLng;

    @Column(name = "capacity_kg")
    Double capacityKg;

    @Column(name = "available_capacity_kg")
    Double availableCapacityKg;

    @Column(name = "departure_time")
    LocalDateTime departureTime;

    @Column(name = "encoded_polyline", columnDefinition = "TEXT")
    String encodedPolyline;

    @Column(name = "estimated_arrival_time")
    LocalDateTime estimatedArrivalTime;

    @Column(name = "total_estimated_distance_km")
    Double totalEstimatedDistanceKm;

    @Column(name = "is_shareable")
    Boolean isShareable;

    @OneToMany(mappedBy = "trip", cascade = CascadeType.ALL, orphanRemoval = true)
    List<TripStop> tripStops;

    @OneToMany(mappedBy = "trip", cascade = CascadeType.ALL, orphanRemoval = true)
    List<TripJoinRequest> tripJoinRequests;
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    TripStatus status;
    Double pricePerKm;
}
