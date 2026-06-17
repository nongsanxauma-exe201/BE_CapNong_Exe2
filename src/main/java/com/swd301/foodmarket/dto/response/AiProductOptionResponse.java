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
    private String unit;
    private Double availableQuantity;
    private String description;
}
