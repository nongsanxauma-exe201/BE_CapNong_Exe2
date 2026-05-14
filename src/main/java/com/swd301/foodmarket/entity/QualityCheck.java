package com.swd301.foodmarket.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

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
    private String qualityCheckId;
    private boolean isPassed;
    @OneToMany(mappedBy = "qualityCheck")
    List<Image> images = new ArrayList<>();


}
