package com.swd301.foodmarket.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderResponse {
    Integer id;

    String recipientName;
    String recipientPhone;
    String shippingAddress;

    String paymentMethod;
    String note;

    BigDecimal shippingFee;
    BigDecimal totalAmount;

    String status;

    LocalDateTime createdAt;

    List<OrderItemResponse> items;
    Integer buyerId;

}
