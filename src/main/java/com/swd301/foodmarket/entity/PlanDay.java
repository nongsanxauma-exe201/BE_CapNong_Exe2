package com.swd301.foodmarket.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PlanDay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Integer dayIndex; // Day 1, Day 2

    @ManyToOne
    private BuildPlan plan;

    @OneToMany(mappedBy = "day", cascade = CascadeType.ALL)
    private List<Meal> meals;
}