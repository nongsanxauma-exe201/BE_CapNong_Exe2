package com.swd301.foodmarket.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "trip_tracking")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TripTracking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id;


    @ManyToOne
    Trip trip;

    Double lat;

    Double lng;

    LocalDateTime createdAt;
}