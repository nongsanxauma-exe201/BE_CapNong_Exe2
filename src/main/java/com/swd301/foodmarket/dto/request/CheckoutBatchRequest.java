package com.swd301.foodmarket.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CheckoutBatchRequest {
    String recipientName;
    String recipientPhone;
    String shippingAddress;
    String note;
    String paymentMethod;
    List<ShopOrderRequest> shopOrders;
    java.util.UUID batchVoucherUserId;
}
