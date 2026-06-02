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

    Double requestedKg;

    Double pickupLat;

    Double pickupLng;


}
