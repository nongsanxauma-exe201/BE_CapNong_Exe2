package com.swd301.foodmarket.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NotificationRequest {
    String title;
    String message;
    String evidence; // Optional: link/proof

    // ============= ADMIN GỬI CHO NHIỀU NHÓM =============
    // Danh sách role nhận thông báo: BUYER, SHOP_OWNER, SHIPPER
    // Ví dụ: ["BUYER", "SHIPPER"] hoặc ["BUYER", "SHOP_OWNER", "SHIPPER"]
    Set<String> receiverTypes;

}