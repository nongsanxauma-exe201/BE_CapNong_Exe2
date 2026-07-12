package com.swd301.foodmarket.mapper;

import com.swd301.foodmarket.dto.response.OrderItemResponse;
import com.swd301.foodmarket.dto.response.OrderResponse;
import com.swd301.foodmarket.entity.*;
import org.mapstruct.*;

import java.util.ArrayList;
import java.util.List;

@Mapper(componentModel = "spring", uses = {TrackingMapper.class, UserMapper.class, QualityResultMapper.class})
public interface OrderMapper {

    // ================== ORDER ==================
    @Mapping(target = "status", source = "status")
    @Mapping(target = "items", expression = "java(mapItems(order))")
    @Mapping(target = "buyerId", source = "buyer.id")
    @Mapping(target = "passedQualityCheck", expression = "java(allPassQualityChecks(order))")
    @Mapping(target = "rescuedKg", source = "rescuedWeight")
    @Mapping(target = "checkoutBatchId", source = "checkoutBatchFkId")
    @Mapping(target = "batchUnpaidCount", ignore = true)
    @Mapping(target = "checkoutBatchStatus", ignore = true)
    OrderResponse toResponse(Order order);

    // ================== PRODUCT ==================
    @Mapping(target = "orderDetailId", source = "id")
    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productName", source = "product.productName")
    @Mapping(target = "quantity", source = "quantity")
    @Mapping(target = "quantityKg", source = "quantityKg")
    @Mapping(target = "unitPrice", source = "unitPrice")
    @Mapping(target = "pricePerKg", source = "pricePerKg")
    @Mapping(target = "unitWeightKg", source = "unitWeightKg")
    @Mapping(target = "rescuedWeightKg", source = "rescuedWeightKg")
    @Mapping(target = "imageUrl", source = "product.imageUrl")
    @Mapping(target = "itemType", constant = "PRODUCT")
    @Mapping(target = "isRequestedReturn", source = "isRequestedReturn")
    @Mapping(target = "unit", constant = "kg")
    OrderItemResponse toItemResponse(OrderDetail detail);

    @AfterMapping
    default void mapReturnStatus(OrderDetail detail, @MappingTarget OrderItemResponse response) {
        if (detail.getReturnRequests() != null && !detail.getReturnRequests().isEmpty()) {
            ReturnRequest latest = detail.getReturnRequests().get(detail.getReturnRequests().size() - 1);
            if (latest.getStatus() != null) {
                response.setReturnStatus(latest.getStatus().name());
            }
        }
    }

    default boolean allPassQualityChecks(Order order) {
        if (order.getOrderDetails() == null) return true; // Nếu không có sản phẩm nào, coi như đã qua QC

        int count = 0;
        for (OrderDetail detail : order.getOrderDetails()) {
            if (detail.getQualityResult() != null && detail.getQualityResult().getQualityResult().equals(com.swd301.foodmarket.enums.QualityResult.APPROVED)) {
                count++;
            }
        }
        return  count == order.getOrderDetails().size();
    }

    // ================== MYSTERY BOX ==================
    @Mapping(target = "mysteryBoxId", source = "mysteryBox.id")
    @Mapping(target = "productName", source = "mysteryBox.boxType")
    @Mapping(target = "quantity", source = "quantity")
    @Mapping(target = "unitPrice", source = "mysteryBox.price")
    @Mapping(target = "unitWeightKg", source = "unitWeightKg")
    @Mapping(target = "rescuedWeightKg", source = "rescuedWeightKg")
    @Mapping(target = "imageUrl", source = "mysteryBox.imageUrl")
    @Mapping(target = "itemType", constant = "MYSTERY_BOX")
    @Mapping(target = "orderMysteryBoxId", source = "id")
    OrderItemResponse toMysteryBoxItemResponse(OrderMysteryBox orderBox);

    // ================== AFTER MAPPING ==================
    default List<OrderItemResponse> mapItems(Order order) {
        List<OrderItemResponse> result = new java.util.ArrayList<>();

        if (order.getOrderDetails() != null) {
            order.getOrderDetails().stream()
                    .map(this::toItemResponse)
                    .forEach(result::add);
        }

        if (order.getMysteryBoxes() != null) {
            order.getMysteryBoxes().stream()
                    .map(this::toMysteryBoxItemResponse)
                    .forEach(result::add);
        }

        return result;
    }
}