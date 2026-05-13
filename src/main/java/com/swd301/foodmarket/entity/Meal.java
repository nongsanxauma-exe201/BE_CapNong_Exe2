package com.swd301.foodmarket.entity;

import com.swd301.foodmarket.enums.MealType;
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
public class Meal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private MealType mealType; // BREAKFAST / LUNCH / DINNER / SNACK

    @ManyToOne
    private PlanDay day;

    @OneToMany(mappedBy = "meal", cascade = CascadeType.ALL)
    private List<MealItem> items;
}
