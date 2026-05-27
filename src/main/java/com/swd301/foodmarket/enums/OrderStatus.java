package com.swd301.foodmarket.enums;

public enum OrderStatus {
    PENDING,
    CONFIRMED,
    READY_FOR_PICKUP,
    SHIPPING,
    SHIPPING_TO_WAREHOUSE,
    ARRIVED_AT_WAREHOUSE,
    QUALITY_CHECKING,
    QUALITY_CHECKED,
    READY,      // Đã đóng gói xong, chờ shipper lấy
    DELIVERED,
    CANCELLED,
    PAID,
    FAILED
}
