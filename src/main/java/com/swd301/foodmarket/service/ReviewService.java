package com.swd301.foodmarket.service;

import com.swd301.foodmarket.dto.request.ReviewCreateRequest;
import com.swd301.foodmarket.dto.request.ReviewReplyRequest;
import com.swd301.foodmarket.dto.request.ReviewUpdateRequest;
import com.swd301.foodmarket.dto.response.PageResponse;
import com.swd301.foodmarket.dto.response.ReviewResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ReviewService {
    ReviewResponse createReview(ReviewCreateRequest request, MultipartFile evidenceFile);

    ReviewResponse updateReview(Integer reviewId, ReviewUpdateRequest request, MultipartFile evidenceFile);

    void deleteReview(Integer reviewId);

    ReviewResponse getReviewById(Integer reviewId);

    List<ReviewResponse> getAllReviews();

    List<ReviewResponse> getReviewsByShop(Integer shopId);
    List<ReviewResponse> getReviewsByProduct(Integer productId);
    PageResponse<ReviewResponse> getReviewsByShopPaged(Integer shopId, int page, int size);

    ReviewResponse replyReview(Integer reviewId, ReviewReplyRequest request);
}
