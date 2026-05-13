package com.swd301.foodmarket.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReviewCreateRequest {
    Integer orderDetailId;

    Integer mysteryBoxId;

    BigDecimal ratingStar;

    String comment;

    String evidence;
}
