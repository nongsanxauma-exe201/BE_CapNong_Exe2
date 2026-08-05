package com.swd301.foodmarket.dto.request;

import com.swd301.foodmarket.enums.DeliveryType;
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
    /** placeId từ Goong suggestion — dùng cùng cách tính phí ship như preview UI */
    String placeId;
    String note;
    String paymentMethod;
    List<ShopOrderRequest> shopOrders;
    java.util.UUID batchVoucherUserId;
    DeliveryType deliveryType; // null → mặc định DELIVERY
}
