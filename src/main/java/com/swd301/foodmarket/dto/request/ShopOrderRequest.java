package com.swd301.foodmarket.dto.request;

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
public class ShopOrderRequest {
    Integer shopOwnerId;
    UUID voucherUserId;
    List<UUID> voucherUserIds;
    List<String> voucherCodes;
    List<OrderItemRequest> items;
}
