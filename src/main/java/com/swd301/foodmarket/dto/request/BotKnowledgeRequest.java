package com.swd301.foodmarket.dto.request;

import com.swd301.foodmarket.enums.KnowledgeType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BotKnowledgeRequest {

    @NotNull(message = "Type không được để trống")
    KnowledgeType type;

    @NotBlank(message = "Title không được để trống")
    String title;

    @NotBlank(message = "Content không được để trống")
    String content;

    // Không bắt buộc
    String keywords;

    Boolean active;
}