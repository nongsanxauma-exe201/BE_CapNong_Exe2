package com.swd301.foodmarket.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AvailableShipperResponse {
    Integer id;
    String fullName;
    String phoneNumber;
    String vehicleNumber;
    String logoUrl;
    Double ratingAverage;
    Double latitude;
    Double longitude;
    Integer currentOrderId; // null nếu chưa nhận đơn nào
}
