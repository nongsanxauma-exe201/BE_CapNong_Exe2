package com.swd301.foodmarket.entity;

import com.swd301.foodmarket.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "trackings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class Tracking {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID trackingId;
    @Enumerated(EnumType.STRING)
    private OrderStatus status;
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    @ManyToOne
    @JoinColumn( name = "order_id")
    private Order order;
    @OneToMany(mappedBy = "tracking")
    @Builder.Default
    private List<Image> images  = new ArrayList<>();
}
