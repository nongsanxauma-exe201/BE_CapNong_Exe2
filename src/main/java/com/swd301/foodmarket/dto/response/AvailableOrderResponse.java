package com.swd301.foodmarket.dto.response;

import com.swd301.foodmarket.enums.OrderStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AvailableOrderResponse {
    Integer orderId;
    String shopName;
    String shopAddress;
    String shopPhone;           // ✅ để shipper liên hệ shop nếu cần

    String shippingAddress;
    String recipientName;
    String recipientPhone;

    BigDecimal shippingFee;
    BigDecimal totalAmount;
    OrderStatus status;
    LocalDateTime createdAt;
    LocalDateTime estimatedDeliveryDate;

    // ✅ Khoảng cách shipper → shop (đường shipper cần đi để lấy hàng)
    Double shipToShopKm;

    // ✅ Khoảng cách shop → nhà buyer (đường giao hàng sau khi lấy)
    Double shopToBuyerKm;

    // Giữ lại distanceKm = shipToShopKm để FE cũ không bị lỗi ngay
    // FE sẽ dùng 2 field mới thay thế
    Double distanceKm;
}