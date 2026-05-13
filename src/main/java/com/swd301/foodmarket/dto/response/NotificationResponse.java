package com.swd301.foodmarket.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NotificationResponse {
    Integer id;
    String title;
    String message;
    String evidence;
    LocalDateTime createAt;
    String receiverType;
    Boolean isRead;

    // Thông tin người gửi
    Integer adminId;
    String adminName;

    Integer shopOwnerId;
    String shopOwnerName;

    Integer buyerId;
    String buyerName;
}