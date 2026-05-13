package com.swd301.foodmarket.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderItemResponse {
    Integer orderDetailId;      // null nếu là mystery box
    Integer mysteryBoxId;       // null nếu là sản phẩm thường

    Integer productId;          // null nếu là mystery box
    String  productName;        // tên sản phẩm HOẶC boxType của túi mù
    Integer quantity;
    BigDecimal unitPrice;

    String  itemType;           // "PRODUCT" hoặc "MYSTERY_BOX"
    String  imageUrl;           // ảnh sản phẩm / túi mù
    Boolean isRequestedReturn;  // đã yêu cầu trả hàng chưa
    String  returnStatus;       // trạng thái hoàn tiền (PENDING, COMPLETED, ...)
}