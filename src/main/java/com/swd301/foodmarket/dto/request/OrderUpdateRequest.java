package com.swd301.foodmarket.dto.request;

import com.swd301.foodmarket.enums.OrderStatus;
import lombok.*;

@Data
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class OrderUpdateRequest {
    OrderStatus status;
}