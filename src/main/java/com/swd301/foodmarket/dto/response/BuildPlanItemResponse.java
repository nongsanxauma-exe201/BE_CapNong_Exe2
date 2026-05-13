package com.swd301.foodmarket.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BuildPlanItemResponse {

    Integer id;

    Integer planId;

    String mealName;

    Integer servings;

    LocalDate mealDate;

    Boolean completed;

    List<ProductResponse> products;

}