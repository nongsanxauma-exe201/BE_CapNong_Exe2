package com.swd301.foodmarket.enums;

public enum OrderStatus {
    PENDING,
    AWAITING_PAYMENT,
    CONFIRMED,
    READY_FOR_PICKUP,
    SHIPPING,
    SHIPPING_TO_WAREHOUSE,
    ARRIVED_AT_WAREHOUSE,
    QUALITY_CHECKING,
    QC_PENDING_BUYER,
    QUALITY_CHECKED,
    READY,      // Đã đóng gói xong, chờ shipper lấy
    DELIVERED,
    CANCELLED,
    PAID,
    FAILED
}
