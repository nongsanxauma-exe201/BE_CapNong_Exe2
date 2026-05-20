package com.swd301.foodmarket.dto.request;


import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateTripJoinRequest {



    List<Integer> orderIds;
   // sau nay muon lam nang cao hon de chia order thanh nhieu trip thi them field nay
//    Double requestedKg;

    Double pickupLat;

    Double pickupLng;
    // Optional: If the farmer wants to specify a pickup address or address shop instead of lat/lng
    String address;

}
