package com.swd301.foodmarket.dto.response;

import com.swd301.foodmarket.enums.AiItemType;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiProductOptionResponse {

    private AiItemType itemType;
    private Integer itemId;
    private String name;
    private String imageUrl;
    private BigDecimal price;
    private BigDecimal salePrice;
    private Integer discountPercent;
    private String unit;
    private Double availableQuantity;
    private String description;
}
