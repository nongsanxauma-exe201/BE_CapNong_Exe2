package com.swd301.foodmarket.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Entity
@Table(name = "categories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_id")
    Integer id;

    @Column(nullable = false, length = 100)
    String name;

    @Column(length = 500)
    String description;

    // ================= RELATIONSHIP =================
    // 1 Category → N Products

    @OneToMany(mappedBy = "category")
    @JsonIgnore
    List<Product> products;
}
