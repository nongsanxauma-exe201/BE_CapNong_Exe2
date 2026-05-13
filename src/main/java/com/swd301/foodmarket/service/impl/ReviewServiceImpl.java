package com.swd301.foodmarket.service.impl;

import com.swd301.foodmarket.dto.request.ReviewCreateRequest;
import com.swd301.foodmarket.dto.request.ReviewReplyRequest;
import com.swd301.foodmarket.dto.request.ReviewUpdateRequest;
import com.swd301.foodmarket.dto.response.PageResponse;
import com.swd301.foodmarket.dto.response.ReviewResponse;
import com.swd301.foodmarket.entity.MysteryBox;
import com.swd301.foodmarket.entity.OrderDetail;
import com.swd301.foodmarket.entity.Review;
import com.swd301.foodmarket.entity.User;
import com.swd301.foodmarket.exception.AppException;
import com.swd301.foodmarket.exception.ErrorCode;
import com.swd301.foodmarket.mapper.ReviewMapper;
import com.swd301.foodmarket.repository.MysteryBoxRepository;
import com.swd301.foodmarket.repository.OrderDetailRepository;
import com.swd301.foodmarket.repository.ReviewRepository;
import com.swd301.foodmarket.repository.UserRepository;
import com.swd301.foodmarket.service.CloudinaryService;
import com.swd301.foodmarket.service.ReviewService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ReviewServiceImpl implements ReviewService {
    ReviewRepository reviewRepository;
    OrderDetailRepository orderDetailRepository;
    UserRepository userRepository;
    ReviewMapper reviewMapper;
    CloudinaryService cloudinaryService;
    MysteryBoxRepository mysteryBoxRepository;

    @Override
    public ReviewResponse createReview(ReviewCreateRequest request, MultipartFile evidenceFile) {

        User buyer = getCurrentUser();
        Review review = reviewMapper.toReview(request);

        // ================= REVIEW PRODUCT =================
        if (request.getOrderDetailId() != null) {

            OrderDetail orderDetail = orderDetailRepository.findById(request.getOrderDetailId())
                    .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

            if (reviewRepository.existsByOrderDetailId(request.getOrderDetailId())) {
                throw new AppException(ErrorCode.REVIEW_ALREADY_EXISTS);
            }

            User shopOwner = orderDetail.getProduct().getShopOwner();

            review.setBuyer(buyer);
            review.setShopOwner(shopOwner);
            review.setOrderDetail(orderDetail);
        }

        // ================= REVIEW MYSTERY BOX =================
        else if (request.getMysteryBoxId() != null) {

            MysteryBox box = mysteryBoxRepository.findById(request.getMysteryBoxId())
                    .orElseThrow(() -> new AppException(ErrorCode.MYSTERY_BOX_NOT_FOUND));

            if (reviewRepository.existsByMysteryBoxId(request.getMysteryBoxId())) {
                throw new AppException(ErrorCode.REVIEW_ALREADY_EXISTS);
            }

            review.setBuyer(buyer);
            review.setShopOwner(box.getShopOwner());
            review.setMysteryBox(box);
        }

        else {
            throw new AppException(ErrorCode.INVALID_REVIEW_TARGET);
        }

        // ================= UPLOAD EVIDENCE =================
        if (evidenceFile != null && !evidenceFile.isEmpty()) {
            String url = cloudinaryService.uploadImage(evidenceFile);
            review.setEvidence(url);
        }

        return reviewMapper.toResponse(reviewRepository.save(review));
    }

    @Override
    public ReviewResponse updateReview(Integer reviewId, ReviewUpdateRequest request, MultipartFile evidenceFile) {

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new AppException(ErrorCode.REVIEW_NOT_FOUND));

        if (!review.getBuyer().getId().equals(getCurrentUser().getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        review.setRatingStar(BigDecimal.valueOf(request.getRatingStar()));
        review.setComment(request.getComment());

        if (evidenceFile != null && !evidenceFile.isEmpty()) {
            String url = cloudinaryService.uploadImage(evidenceFile);
            review.setEvidence(url);
        }

        return reviewMapper.toResponse(reviewRepository.save(review));
    }

    @Override
    public void deleteReview(Integer reviewId) {

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new AppException(ErrorCode.REVIEW_NOT_FOUND));

        if (!review.getBuyer().getId().equals(getCurrentUser().getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        reviewRepository.delete(review);
    }

    @Override
    public ReviewResponse getReviewById(Integer reviewId) {

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new AppException(ErrorCode.REVIEW_NOT_FOUND));

        return reviewMapper.toResponse(review);
    }

    @Override
    public List<ReviewResponse> getAllReviews() {

        return reviewRepository.findAll()
                .stream()
                .map(reviewMapper::toResponse)
                .toList();
    }

    @Override
    public List<ReviewResponse> getReviewsByShop(Integer shopId) {

        return reviewRepository.findByShopOwnerId(shopId)
                .stream()
                .map(reviewMapper::toResponse)
                .toList();
    }

    @Override
    public List<ReviewResponse> getReviewsByProduct(Integer productId) {

        return reviewRepository.findByOrderDetailProductId(productId)
                .stream()
                .map(reviewMapper::toResponse)
                .toList();
    }

    @Override
    public PageResponse<ReviewResponse> getReviewsByShopPaged(Integer shopId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<Review> pageResult = reviewRepository.findByShopOwnerId(shopId, pageable);
        return PageResponse.<ReviewResponse>builder()
                .content(pageResult.getContent().stream().map(reviewMapper::toResponse).toList())
                .page(pageResult.getNumber())
                .size(pageResult.getSize())
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .first(pageResult.isFirst())
                .last(pageResult.isLast())
                .build();
    }


    @Override
    public ReviewResponse replyReview(Integer reviewId, ReviewReplyRequest request) {

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new AppException(ErrorCode.REVIEW_NOT_FOUND));

        if (!review.getShopOwner().getId().equals(getCurrentUser().getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        review.setReplyFromShop(request.getReplyFromShop());

        return reviewMapper.toResponse(reviewRepository.save(review));
    }

    private User getCurrentUser() {

        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }
}
