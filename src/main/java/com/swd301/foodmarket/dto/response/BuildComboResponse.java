package com.swd301.foodmarket.dto.response;

import com.swd301.foodmarket.enums.MealType;
import com.swd301.foodmarket.enums.Region;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BuildComboResponse {
    Integer id;

    String comboName;

    BigDecimal discountPrice;

    String description;

    String type;

    Integer shopOwnerId;

    Region region;


    MealType mealType;
    List<ProductComboResponse> items;

    String imageUrl;
}
