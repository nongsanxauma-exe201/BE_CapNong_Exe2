package com.swd301.foodmarket.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReviewShopReport {
    private long totalReviewers;

    private double averageRating;

    private long fiveStarNumber;
    private long fourStarNumber;
    private long threeStarNumber;
    private long twoStarNumber;
    private long oneStarNumber;
}