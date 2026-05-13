package com.swd301.foodmarket.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChatHistoryResponse {
    int id;
    String role;       // "USER" | "BOT"
    String content;
    LocalDateTime createdAt;
}