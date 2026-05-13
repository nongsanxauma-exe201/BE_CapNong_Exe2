package com.swd301.foodmarket.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ShipperLocationRequest {

    @NotNull
    Integer orderId;

    Double latitude;
    Double longitude;

    // ✅ FakeGPS gửi true → BE pass qua response → Tracking trimRoute
    Boolean isFake;

    // ✅ App mobile gửi khi tắt GPS
    Boolean gpsOff;
}