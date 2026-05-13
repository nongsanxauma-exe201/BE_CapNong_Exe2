package com.swd301.foodmarket.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ProductMysteryResponse {

    Integer productId;

    String productName;

    Integer quantity;

    BigDecimal sellingPrice;
}
