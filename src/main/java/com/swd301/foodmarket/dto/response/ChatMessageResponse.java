package com.swd301.foodmarket.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

/**
 * Server gửi xuống client.
 * Dùng cho cả WebSocket broadcast lẫn REST API lấy lịch sử.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChatMessageResponse {
    Integer id;
    Integer conversationId;
    Integer senderId;
    String senderName;
    String content;
    LocalDateTime sentAt;
    boolean isRead;
}