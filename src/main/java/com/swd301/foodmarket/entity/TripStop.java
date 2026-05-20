package com.swd301.foodmarket.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.swd301.foodmarket.enums.TripStopType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "trip_stops")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TripStop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)

    Trip trip;

    @Column(name = "sequence")
    Integer sequence;

    @Column(name = "lat")
    Double lat;

    @Column(name = "lng")
    Double lng;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "farmer_id")

    User farmer;

    @Column(name = "stop_type")
    @Enumerated(EnumType.STRING)
    TripStopType stopType;
    @OneToMany(mappedBy = "tripStop")
    @Builder.Default
    List<Order> order = new ArrayList<>();
    @Builder.Default
    boolean isPickup = false;
    LocalDateTime actualArrivalTime;
}
