package com.swd301.foodmarket.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderResponse {
    Integer id;
    UserResponse shopOwner;
    String recipientName;
    String recipientPhone;
    String shippingAddress;

    String paymentMethod;
    String note;

    BigDecimal shippingFee;
    BigDecimal totalAmount;
    BigDecimal totalKg;
    BigDecimal rescuedKg;
    BigDecimal rescuedWeight;
    BigDecimal rewardPoints;
    Boolean rewardApplied;

    String status;

    LocalDateTime createdAt;

    List<OrderItemResponse> items;

    Integer buyerId;

    List<TrackingResponse> trackings;

    boolean passedQualityCheck;

    BigDecimal refundAmount;

    String refundReason;

    BigDecimal codAmount;

    Boolean codSettled;

    LocalDateTime codSettledAt;

}
