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
    /** PENDING | PAID | CANCELLED ... — dùng FE phân biệt hủy cả batch PayOS vs hủy từng đơn đã TT. */
    String checkoutBatchStatus;
    Integer batchUnpaidCount;
    Integer batchShopCount;
    Boolean hasPlatformBatchVoucher;
    BigDecimal batchPendingTotalAmount;
    /** Tổng PayOS / checkout batch (khớp số tiền thanh toán thực tế). */
    BigDecimal batchCheckoutTotalAmount;
    /** Tổng giảm voucher sàn trên batch (nếu có). */
    BigDecimal batchPlatformDiscountAmount;
    /** Phần giảm voucher sàn phân bổ trên đơn này. */
    BigDecimal batchDiscountAmount;

    BigDecimal qcRefundedAmount;
    LocalDateTime qcBuyerDecisionDeadline;
    Boolean qcBuyerAcceptedRemaining;
}
