package com.swd301.foodmarket.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "quality_crieria")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QualityCrieria {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer qualityCrieriaId;
    private String name;
    private String description;

}
