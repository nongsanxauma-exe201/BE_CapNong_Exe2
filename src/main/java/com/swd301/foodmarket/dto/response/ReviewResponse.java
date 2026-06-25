package com.swd301.foodmarket.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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


    String boxType ; // name of mytery box

    BigDecimal ratingStar;

    Integer productId;

    String productName;

    String comment;

    String evidence;

    String replyFromShop;

    Long quantity;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime createAt;

    Long likeCount;

    Long dislikeCount;

    String currentUserReaction;
}
