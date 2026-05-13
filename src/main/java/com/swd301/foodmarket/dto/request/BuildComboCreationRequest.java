package com.swd301.foodmarket.dto.request;


import com.swd301.foodmarket.enums.MealType;
import com.swd301.foodmarket.enums.Region;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.List;


@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BuildComboCreationRequest {
    String comboName;

    BigDecimal discountPrice;

    String description;

    String type;

    Region region;

    MealType mealType;

    List<ProductComboRequest> items;

    String imageUrl;
}
