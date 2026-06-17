package com.swd301.foodmarket.dto.response;

import com.swd301.foodmarket.enums.AiItemType;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiCartItemResponse {

    private AiItemType itemType;
    private Integer itemId;
    private String name;
    private String imageUrl;
    private Double quantity;
    private String unit;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
}
