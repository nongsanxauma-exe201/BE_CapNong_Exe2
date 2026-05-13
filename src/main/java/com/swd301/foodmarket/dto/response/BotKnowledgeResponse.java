package com.swd301.foodmarket.dto.response;

import com.swd301.foodmarket.enums.KnowledgeType;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BotKnowledgeResponse {
    Integer id;
    KnowledgeType type;
    String title;
    String content;
    String keywords;
    Boolean active;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}