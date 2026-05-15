package com.swd301.foodmarket.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "quality_check")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QualityCheck {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID qualityCheckId;
    private boolean isPassed;
    @OneToMany(mappedBy = "qualityCheck")
    private List<Image> images = new ArrayList<>();
    @ManyToOne
    @JoinColumn(name = "quality_criteria_id")
    private QualityCriteria qualityCriteria;
    @ManyToOne
    @JoinColumn(name = "quality_result_id")
    private QualityResult qualityResult;
}
