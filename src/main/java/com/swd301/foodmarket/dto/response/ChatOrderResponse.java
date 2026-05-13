package com.swd301.foodmarket.dto.response;

import com.swd301.foodmarket.enums.OrderStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Thông tin đơn hàng hiển thị trong khung chat.
 * 1 Order gồm 3 loại item: OrderDetail (product) + BuildCombo + MysteryBox
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChatOrderResponse {

    Integer orderId;
    OrderStatus status;
    BigDecimal totalAmount;
    LocalDateTime createdAt;

    // Item đại diện để preview trong chat (ưu tiên product → combo → mystery)
    String previewItemName;
    String previewImageUrl;
    int totalItems; // tổng số items trong đơn (details + combos + mysteries)

    // Chi tiết từng loại
    List<OrderItemPreview> orderDetails;  // sản phẩm thường
    List<OrderItemPreview> combos;        // build combo
    List<OrderItemPreview> mysteryBoxes;  // túi mù

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemPreview {
        String name;
        String imageUrl;
        Integer quantity;     // null với mystery box
        BigDecimal unitPrice;
        String type;          // "PRODUCT" | "COMBO" | "MYSTERY_BOX"
    }
}