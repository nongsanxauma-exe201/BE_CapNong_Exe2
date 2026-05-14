package com.swd301.foodmarket.entity;

import com.swd301.foodmarket.enums.OrderStatus;
import com.swd301.foodmarket.enums.QualityResult;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "images")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class Image {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID imageId;
    private String imageUrl;
    @ManyToOne
    @JoinColumn(name = "tracking_id")
    private Tracking tracking;
    @ManyToOne
    @JoinColumn(name = "quality_check_id")
    private QualityCheck qualityCheck;
}
