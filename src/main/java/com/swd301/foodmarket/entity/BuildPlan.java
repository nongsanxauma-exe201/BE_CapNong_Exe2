package com.swd301.foodmarket.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "build_plans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BuildPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "plan_id")
    private Integer id;

    @ManyToOne
    private User buyer;

    private String planName;

    private Integer numberOfPeople;   // số người ăn
    private Integer numberOfDays;     // số ngày
    private String mealType;          // Gia đình / Giảm cân / Ăn chay

    private BigDecimal targetBudget;

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PlanDay> days;

    @OneToMany(mappedBy = "buildPlan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BuildPlanItem> buildPlanItems;
}