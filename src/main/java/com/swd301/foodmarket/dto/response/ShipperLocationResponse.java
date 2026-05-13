package com.swd301.foodmarket.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ShipperLocationResponse {
    Integer shipperId;
    String shipperName;
    Integer orderId;

    Double latitude;
    Double longitude;
    LocalDateTime updatedAt;

    Double shopLatitude;
    Double shopLongitude;

    Double destLatitude;
    Double destLongitude;

    // ✅ Pass-through từ FakeGPS để Tracking biết có trim route không
    Boolean isFake;
}