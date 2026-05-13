package com.swd301.foodmarket.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "not_id")
    Integer id;

    // ================= RELATIONSHIPS =================

    // Người gửi (Admin)
    @ManyToOne
    @JoinColumn(name = "admin_id")
    @JsonIgnore
    User admin;

    // Danh sách người nhận được quản lý qua bảng user_notifications
    @OneToMany(mappedBy = "notification", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    java.util.List<UserNotification> userNotifications;

    // ================= DATA =================

    @Column(length = 50, nullable = false)
    String title;

    @Column(columnDefinition = "TEXT")
    String message;

    @Column(length = 500)
    String evidence;

    @Column(name = "create_at")
    LocalDateTime createAt;

    // Lưu danh sách role nhận, cách nhau bởi dấu phẩy: "BUYER,SHIPPER" hoặc "BUYER,SHOP_OWNER,SHIPPER"
    // Hoặc lưu userId cụ thể khi gửi 1-1: "USER_123"
    @Column(name = "receiver_type", length = 100)
    String receiverType;

    @PrePersist
    void onCreate() {
        createAt = LocalDateTime.now();
    }
}