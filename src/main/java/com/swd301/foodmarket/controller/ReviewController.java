package com.swd301.foodmarket.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swd301.foodmarket.dto.request.ReviewCreateRequest;
import com.swd301.foodmarket.dto.request.ReviewReplyRequest;
import com.swd301.foodmarket.dto.request.ReviewUpdateRequest;
import com.swd301.foodmarket.dto.response.ApiResponse;
import com.swd301.foodmarket.dto.response.PageResponse;
import com.swd301.foodmarket.dto.response.ReviewResponse;
import com.swd301.foodmarket.exception.AppException;
import com.swd301.foodmarket.exception.ErrorCode;
import com.swd301.foodmarket.service.ReviewService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ReviewController {
    ReviewService reviewService;

    @GetMapping
    public ApiResponse<List<ReviewResponse>> getAll() {
        return ApiResponse.<List<ReviewResponse>>builder()
                .result(reviewService.getAllReviews())
                .build();
    }


    @GetMapping("/{id}")
    public ApiResponse<ReviewResponse> getById(@PathVariable Integer id) {
        return ApiResponse.<ReviewResponse>builder()
                .result(reviewService.getReviewById(id))
                .build();
    }


    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ReviewResponse> createReview(
            @RequestPart("data") String data,
            @RequestPart(value = "evidence", required = false) MultipartFile evidence
    ) throws Exception {

        ObjectMapper objectMapper = new ObjectMapper();
        ReviewCreateRequest request =
                objectMapper.readValue(data, ReviewCreateRequest.class);

        if (request.getOrderDetailId() == null && request.getMysteryBoxId() == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        if (request.getOrderDetailId() != null && request.getMysteryBoxId() != null) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        return ApiResponse.<ReviewResponse>builder()
                .result(reviewService.createReview(request, evidence))
                .build();
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ReviewResponse> updateReview(
            @PathVariable Integer id,
            @RequestPart("data") String data,
            @RequestPart(value = "evidence", required = false) MultipartFile evidence
    ) throws Exception {

        ObjectMapper objectMapper = new ObjectMapper();
        ReviewUpdateRequest request =
                objectMapper.readValue(data, ReviewUpdateRequest.class);

        return ApiResponse.<ReviewResponse>builder()
                .result(reviewService.updateReview(id, request, evidence))
                .build();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteReview(@PathVariable Integer id) {

        log.info("Delete review id={}", id);

        reviewService.deleteReview(id);

        return ApiResponse.<Void>builder()
                .message("REVIEW_DELETED")
                .build();
    }

    @PostMapping("/{id}/reply")
    public ApiResponse<ReviewResponse> replyReview(
            @PathVariable Integer id,
            @RequestBody ReviewReplyRequest request
    ) {

        return ApiResponse.<ReviewResponse>builder()
                .result(reviewService.replyReview(id, request))
                .build();
    }

    @GetMapping("/shop/{shopId}")
    public ApiResponse<List<ReviewResponse>> getByShopId(
            @PathVariable Integer shopId
    ) {
        return ApiResponse.<List<ReviewResponse>>builder()
                .result(reviewService.getReviewsByShop(shopId))
                .build();
    }

    @GetMapping("/product/{productId}")
    public ApiResponse<List<ReviewResponse>> getByProductId(
            @PathVariable Integer productId
    ) {
        return ApiResponse.<List<ReviewResponse>>builder()
                .result(reviewService.getReviewsByProduct(productId))
                .build();
    }

    @GetMapping("/shop/{shopId}/paged")
    public ApiResponse<PageResponse<ReviewResponse>> getByShopIdPaged(
            @PathVariable Integer shopId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.<PageResponse<ReviewResponse>>builder()
                .result(reviewService.getReviewsByShopPaged(shopId, page, size))
                .build();
    }
}
