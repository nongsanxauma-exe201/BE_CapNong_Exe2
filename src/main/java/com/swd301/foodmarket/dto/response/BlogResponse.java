package com.swd301.foodmarket.dto.response;

import com.swd301.foodmarket.enums.BlogStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BlogResponse {

    Integer id;
    String title;
    String content;
    String pictureUrl;
    String category;
    BlogStatus status;
    LocalDateTime createAt;

    // Admin info
    Integer adminId;
    String adminName;
    Integer views;
}