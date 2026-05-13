package com.swd301.foodmarket.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AddCartItemRequest {
    private Integer productId;       // null nếu thêm túi mù
    private Integer mysteryBoxId;
    private Integer buildComboId;
    private Integer quantity;
}