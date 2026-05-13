package com.swd301.foodmarket.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReviewResponse {
    Integer id;

    Integer buyerId;

    String fullName;

    Integer shopOwnerId;

    Integer orderDetailId;

    Integer mysteryBoxId;

    BigDecimal ratingStar;

    Integer productId;

    String productName;

    String comment;

    String evidence;

    String replyFromShop;
}
