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
    private BigDecimal unitPrice;       // Giá hiệu lực (salePrice nếu đang giảm, ngược lại price)
    private BigDecimal originalPrice;   // Giá gốc (null nếu không có giảm giá)
    private Integer discountPercent;    // % giảm (null nếu không có giảm giá)
    private BigDecimal totalPrice;
}
