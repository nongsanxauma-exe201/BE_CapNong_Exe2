package com.swd301.foodmarket.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.swd301.foodmarket.enums.CodPaymentOption;
import com.swd301.foodmarket.enums.CodPaymentStatus;
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
    UserResponse shipper;
    String recipientName;
    String recipientPhone;
    String shippingAddress;

    String paymentMethod;
    String note;

    BigDecimal shippingFee;
    BigDecimal shippingDiscountAmount;
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
    BigDecimal codPrepaidAmount;
    CodPaymentOption codPaymentOption;
    CodPaymentStatus codPaymentStatus;

    Boolean codSettled;

    LocalDateTime codSettledAt;

    Integer checkoutBatchId;
    Integer batchUnpaidCount;
    Integer batchShopCount;
    Boolean hasPlatformBatchVoucher;
    BigDecimal batchPendingTotalAmount;

    BigDecimal qcRefundedAmount;
    LocalDateTime qcBuyerDecisionDeadline;
    Boolean qcBuyerAcceptedRemaining;
}
