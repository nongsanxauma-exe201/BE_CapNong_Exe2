package com.swd301.foodmarket.mapper;

import com.swd301.foodmarket.dto.response.OrderItemResponse;
import com.swd301.foodmarket.dto.response.OrderResponse;
import com.swd301.foodmarket.entity.*;
import org.mapstruct.*;

import java.util.ArrayList;
import java.util.List;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    // ================== ORDER ==================
    @Mapping(target = "status", source = "status")
    @Mapping(target = "items", expression = "java(mapItems(order))")
    @Mapping(target = "buyerId", source = "buyer.id")
    OrderResponse toResponse(Order order);

    // ================== PRODUCT ==================
    @Mapping(target = "orderDetailId", source = "id")
    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productName", source = "product.productName")
    @Mapping(target = "quantity", source = "quantity")
    @Mapping(target = "unitPrice", source = "unitPrice")
    @Mapping(target = "imageUrl", source = "product.imageUrl")
    @Mapping(target = "itemType", constant = "PRODUCT")
    @Mapping(target = "isRequestedReturn", source = "isRequestedReturn")
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

    // ================== MYSTERY BOX ==================
    @Mapping(target = "mysteryBoxId", source = "mysteryBox.id")
    @Mapping(target = "productName", source = "mysteryBox.boxType")
    @Mapping(target = "quantity", source = "quantity")
    @Mapping(target = "unitPrice", source = "mysteryBox.price")
    @Mapping(target = "imageUrl", source = "mysteryBox.imageUrl")
    @Mapping(target = "itemType", constant = "MYSTERY_BOX")
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