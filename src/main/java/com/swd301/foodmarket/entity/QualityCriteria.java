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
public class QualityCriteria {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer qualityCriteriaId;
    private String qualityName;
    private String description;

}
