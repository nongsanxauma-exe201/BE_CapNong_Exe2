package com.swd301.foodmarket.dto.response;

import com.swd301.foodmarket.enums.OrderStatus;
import com.swd301.foodmarket.enums.CodPaymentOption;
import com.swd301.foodmarket.enums.CodPaymentStatus;
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
    BigDecimal totalAmount;
    String paymentMethod;
    BigDecimal codAmount;
    BigDecimal codPrepaidAmount;
    CodPaymentOption codPaymentOption;
    CodPaymentStatus codPaymentStatus;
    BigDecimal refundAmount;
    String refundReason;
    OrderStatus status;
    LocalDateTime createdAt;
    LocalDateTime estimatedDeliveryDate;
    String note;

    // ── Kho bãi (điểm lấy hàng) — lấy thẳng từ bảng warehouses trong DB ────────
    Integer warehouseId;
    String  warehouseName;
    String  warehouseAddress;
    Double  warehouseLat;
    Double  warehouseLng;

    // ── Điểm giao hàng (nhà buyer) ───────────────────────────────────────────────
    Double shippingLatitude;
    Double shippingLongitude;
}
