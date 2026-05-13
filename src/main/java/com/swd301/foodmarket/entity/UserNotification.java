package com.swd301.foodmarket.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "user_notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "notification_id")
    Notification notification;

    @Builder.Default
    @Column(name = "is_read")
    Boolean isRead = false;

    @Builder.Default
    @Column(name = "is_deleted")
    Boolean isDeleted = false;
}
