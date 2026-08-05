package com.swd301.foodmarket.dto.request;

import com.swd301.foodmarket.enums.DeliveryType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderCreationRequest {
    String recipientName;
    String recipientPhone;
    String shippingAddress;
    String placeId;
    DeliveryType deliveryType; // null → mặc định DELIVERY (xử lý ở OrderServiceImpl)
    String note;
    String paymentMethod;
    UUID voucherUserId;
    List<UUID> voucherUserIds;
    List<String> voucherCodes;

    Boolean restrictToShopVouchers;
    Boolean skipCartCleanup;

    List<OrderItemRequest> items;
}
