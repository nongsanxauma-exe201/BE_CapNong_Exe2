package com.swd301.foodmarket.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Client gửi lên qua STOMP WebSocket.
 * Không cần senderId vì server tự lấy từ JWT trong WS session.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChatMessageRequest {
    Integer receiverId; // ID người nhận
    String content;
}