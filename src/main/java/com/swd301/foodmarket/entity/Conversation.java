package com.swd301.foodmarket.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Mỗi Conversation = 1 phòng chat giữa 2 người (buyer-shop, buyer-admin, shop-admin)
 * roomKey = "userId1_userId2" (luôn sort tăng dần để tránh trùng)
 * VD: user 3 và user 7 → roomKey = "3_7"
 */
@Entity
@Table(name = "conversations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id;

    // VD: "3_7" (user id 3 và user id 7, luôn min_max)
    @Column(name = "room_key", unique = true, nullable = false)
    String roomKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user1_id", nullable = false)
    User user1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user2_id", nullable = false)
    User user2;

    @Column(name = "created_at")
    LocalDateTime createdAt;

    @Column(name = "last_message_at")
    LocalDateTime lastMessageAt;

    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL)
    List<ChatMessage> messages;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        lastMessageAt = LocalDateTime.now();
    }
}