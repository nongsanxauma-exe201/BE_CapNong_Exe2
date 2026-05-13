package com.swd301.foodmarket.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChatResponse {
    String reply;
    LocalDateTime timestamp;

    // Danh sách sản phẩm liên quan (có thể rỗng)
    List<ProductSuggestion> suggestedProducts;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductSuggestion {
        Integer id;
        String name;
        BigDecimal price;
        String imageUrl;
        String category;
        // FE dùng id này để navigate: /products/{id}
    }
}