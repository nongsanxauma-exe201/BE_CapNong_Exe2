package com.swd301.foodmarket.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

/**
 * Thông tin 1 cuộc hội thoại trong danh sách inbox.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ConversationResponse {
    Integer id;
    String roomKey;

    // Thông tin người còn lại (không phải mình)
    Integer otherUserId;
    String otherUserName;
    String otherUserRole;

    String lastMessage;
    LocalDateTime lastMessageAt;
    int unreadCount;
}