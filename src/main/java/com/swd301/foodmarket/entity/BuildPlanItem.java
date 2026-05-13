package com.swd301.foodmarket.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.util.Set;

@Entity
@Table(name = "build_plan_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BuildPlanItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id;

    @ManyToOne
    @JoinColumn(name = "plan_id")
    BuildPlan buildPlan;

    String mealName;

    Integer servings;

    LocalDate mealDate;

    Boolean completed;

    @ManyToMany
    @JoinTable(
            name = "meal_products",
            joinColumns = @JoinColumn(name = "meal_id"),
            inverseJoinColumns = @JoinColumn(name = "product_id")
    )
    Set<Product> products;
}