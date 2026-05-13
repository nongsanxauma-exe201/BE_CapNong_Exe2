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
public class ShipperOrderResponse {
    Integer orderId;
    String buyerName;
    String buyerPhone;
    String shippingAddress;
    String recipientName;
    String recipientPhone;
    BigDecimal shippingFee;
    OrderStatus status;
    LocalDateTime createdAt;
    LocalDateTime estimatedDeliveryDate;
    String note;

    // ✅ Tọa độ shop (điểm lấy hàng) — dùng cho FakeGPS
    Double shopLatitude;
    Double shopLongitude;

    // ✅ Tọa độ nhà buyer (điểm giao hàng) — dùng cho FakeGPS
    Double shippingLatitude;
    Double shippingLongitude;
}