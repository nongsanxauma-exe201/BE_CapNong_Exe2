package com.swd301.foodmarket.dto.response;

import com.swd301.foodmarket.enums.TripJoinStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TripJoinRequestResponse {

    Integer id;

    Integer tripId;

    Integer farmerId;

    String farmerName;

    List<OrderResponse> orders;

    Double requestedKg;

    Double pickupLat;

    Double pickupLng;

    TripJoinStatus status;

    BigDecimal priceOffer;

    LocalDateTime createdAt;

    TripResponse trip;
}