package com.swd301.foodmarket.dto.response;

import com.swd301.foodmarket.entity.Order;
import com.swd301.foodmarket.entity.Trip;
import com.swd301.foodmarket.entity.User;
import com.swd301.foodmarket.enums.TripStopType;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class TripStopResponse {

    Integer id;
    Integer sequence;
    Double lat;
    Double lng;
    UserResponse farmer;
    TripStopType stopType;


    List<OrderResponse> orderResponses ;
    boolean isPickup ;
    LocalDateTime actualArrivalTime;
    BigDecimal priceForStop;
}
