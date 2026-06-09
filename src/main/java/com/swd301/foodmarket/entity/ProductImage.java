package com.swd301.foodmarket.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "product_images", indexes = {
        @Index(name = "idx_product_images_product_id", columnList = "product_id"),
        @Index(name = "idx_product_images_order", columnList = "product_id, display_order")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    Product product;

    @Column(name = "image_url", nullable = false, length = 500)
    String imageUrl;

    @Column(name = "public_id", nullable = false, length = 200)
    String publicId;

    @Column(name = "is_primary", nullable = false)
    Boolean isPrimary;

    @Column(name = "display_order", nullable = false)
    Integer displayOrder;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;
}
