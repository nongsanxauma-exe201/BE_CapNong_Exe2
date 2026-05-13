package com.swd301.foodmarket.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BuildPlanItemCreateRequest {

    String mealName;

    Integer servings;

    LocalDate mealDate;

    List<Integer> productIds; // sản phẩm dùng cho món ăn

}