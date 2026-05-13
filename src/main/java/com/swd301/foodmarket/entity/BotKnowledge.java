package com.swd301.foodmarket.entity;

import com.swd301.foodmarket.enums.KnowledgeType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Entity
@Table(name = "bot_knowledge")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BotKnowledge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id;

    // Loại kiến thức: PRODUCT, POLICY, FAQ, COMBO
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    KnowledgeType type;

    // Tiêu đề ngắn, dùng để tìm kiếm keyword
    // VD: "phí giao hàng", "đổi trả sản phẩm lỗi", "combo bò lúc lắc"
    @Column(nullable = false)
    String title;

    // Nội dung chi tiết để đưa vào prompt
    @Column(columnDefinition = "TEXT", nullable = false)
    String content;

    // Từ khóa phụ giúp matching tốt hơn, phân cách bằng dấu phẩy
    // VD: "giao hàng, ship, phí ship, vận chuyển"
    @Column(columnDefinition = "TEXT")
    String keywords;

    @Column(nullable = false)
    Boolean active;

    LocalDateTime createdAt;
    LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.active == null) this.active = true;
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}