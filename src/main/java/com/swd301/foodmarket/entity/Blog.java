package com.swd301.foodmarket.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.swd301.foodmarket.enums.BlogStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Entity
@Table(name = "blogs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Blog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "blog_id")
    Integer id;

    // ================= RELATIONSHIP =================
    // N Blog → 1 Admin (User)

    @ManyToOne
    @JoinColumn(name = "admin_id")
    @JsonIgnore
    User admin;

    // ================= DATA =================

    @Column(length = 50, nullable = false)
    String title;

    @Column(columnDefinition = "TEXT")
    String content;

    @Column(name = "picture_url", length = 500)
    String pictureUrl;

    @Enumerated(EnumType.STRING)
    BlogStatus status;

    @Column(length = 100)
    String category;

    @Column(name = "create_at")
    LocalDateTime createAt;

    @Column(name = "views")
    Integer views = 0;

    @PrePersist
    void onCreate() {
        createAt = LocalDateTime.now();
    }
}
