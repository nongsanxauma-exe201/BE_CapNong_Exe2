package com.swd301.foodmarket.service.impl;

import com.swd301.foodmarket.dto.request.ReviewCreateRequest;
import com.swd301.foodmarket.dto.request.ReviewReactionRequest;
import com.swd301.foodmarket.dto.request.ReviewReplyRequest;
import com.swd301.foodmarket.dto.request.ReviewUpdateRequest;
import com.swd301.foodmarket.dto.response.PageResponse;
import com.swd301.foodmarket.dto.response.ReviewResponse;
import com.swd301.foodmarket.dto.response.ReviewShopReport;
import com.swd301.foodmarket.entity.*;
import com.swd301.foodmarket.enums.OrderStatus;
import com.swd301.foodmarket.enums.ReactionType;
import com.swd301.foodmarket.exception.AppException;
import com.swd301.foodmarket.exception.ErrorCode;
import com.swd301.foodmarket.mapper.ReviewMapper;
import com.swd301.foodmarket.repository.*;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ReviewServiceImpl implements ReviewService {
    ReviewRepository reviewRepository;
    ReviewReactionRepository reviewReactionRepository;
    OrderDetailRepository orderDetailRepository;
    UserRepository userRepository;
    ReviewMapper reviewMapper;
    CloudinaryService cloudinaryService;
    MysteryBoxRepository mysteryBoxRepository;

    @Override
    public ReviewResponse createReview(ReviewCreateRequest request, MultipartFile evidenceFile) {

        User buyer = getCurrentUser();
        Review review = reviewMapper.toReview(request);

        User shopOwner = null;

        // ================= REVIEW PRODUCT =================
        if (request.getOrderDetailId() != null) {

            OrderDetail orderDetail = orderDetailRepository.findById(request.getOrderDetailId())
                    .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

            // Kiểm tra đơn hàng phải ở trạng thái DELIVERED
            if (orderDetail.getOrder().getStatus() != OrderStatus.DELIVERED) {
                throw new AppException(ErrorCode.INVALID_ORDER_STATUS);
            }

            // Kiểm tra đơn hàng thuộc về buyer hiện tại
            if (!orderDetail.getOrder().getBuyer().getId().equals(buyer.getId())) {
                throw new AppException(ErrorCode.UNAUTHORIZED);
            }

            if (reviewRepository.existsByOrderDetailId(request.getOrderDetailId())) {
                throw new AppException(ErrorCode.REVIEW_ALREADY_EXISTS);
            }

            shopOwner = orderDetail.getProduct().getShopOwner();

            review.setBuyer(buyer);
            review.setCreateAt(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")));
            review.setShopOwner(shopOwner);
            review.setOrderDetail(orderDetail);
        }

        // ================= REVIEW MYSTERY BOX =================
        else if (request.getMysteryBoxId() != null) {

            MysteryBox box = mysteryBoxRepository.findById(request.getMysteryBoxId())
                    .orElseThrow(() -> new AppException(ErrorCode.MYSTERY_BOX_NOT_FOUND));

            // Kiểm tra buyer chưa review mystery box này (per-buyer)
            if (reviewRepository.existsByMysteryBoxIdAndBuyerId(request.getMysteryBoxId(), buyer.getId())) {
                throw new AppException(ErrorCode.REVIEW_ALREADY_EXISTS);
            }

            shopOwner = box.getShopOwner();
            review.setBuyer(buyer);
            review.setCreateAt(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")));
            review.setShopOwner(shopOwner);
            review.setMysteryBox(box);
        } else {
            throw new AppException(ErrorCode.INVALID_REVIEW_TARGET);
        }

        // ================= UPLOAD EVIDENCE =================
        if (evidenceFile != null && !evidenceFile.isEmpty()) {
            String url = cloudinaryService.uploadImage(evidenceFile);
            review.setEvidence(url);
        }

        Review reviewSave = reviewRepository.save(review);
        updateShopRating(shopOwner);

        return toResponseWithReactions(reviewSave, buyer);
    }

    private void updateShopRating(User shopOwner) {
        Double averageRating = reviewRepository.getAverageRatingByShopOwner(shopOwner);
        shopOwner.setRatingAverage(averageRating != null ? averageRating : 0.0);
        userRepository.save(shopOwner);
    }

    @Override
    public ReviewResponse updateReview(Integer reviewId, ReviewUpdateRequest request, MultipartFile evidenceFile) {

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new AppException(ErrorCode.REVIEW_NOT_FOUND));

        User buyer = getCurrentUser();
        if (!review.getBuyer().getId().equals(buyer.getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        review.setRatingStar(BigDecimal.valueOf(request.getRatingStar()));
        review.setComment(request.getComment());

        if (evidenceFile != null && !evidenceFile.isEmpty()) {
            String url = cloudinaryService.uploadImage(evidenceFile);
            review.setEvidence(url);
        }

        Review saved = reviewRepository.save(review);
        updateShopRating(saved.getShopOwner());
        return toResponseWithReactions(saved, buyer);
    }

    @Override
    public void deleteReview(Integer reviewId) {

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new AppException(ErrorCode.REVIEW_NOT_FOUND));

        User currentUser = getCurrentUser();
        if (!review.getBuyer().getId().equals(currentUser.getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        User shopOwner = review.getShopOwner();
        reviewRepository.delete(review);
        updateShopRating(shopOwner);
    }

    @Override
    public ReviewResponse getReviewById(Integer reviewId) {

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new AppException(ErrorCode.REVIEW_NOT_FOUND));

        return toResponseWithReactions(review, getCurrentUserSafe());
    }

    @Override
    public List<ReviewResponse> getAllReviews() {
        User currentUser = getCurrentUserSafe();
        return reviewRepository.findAll()
                .stream()
                .map(r -> toResponseWithReactions(r, currentUser))
                .toList();
    }

    @Override
    public List<ReviewResponse> getReviewsByShop(Integer shopId) {
        User currentUser = getCurrentUserSafe();
        return reviewRepository.findByShopOwnerId(shopId)
                .stream()
                .map(r -> toResponseWithReactions(r, currentUser))
                .toList();
    }

    @Override
    public List<ReviewResponse> getReviewsByProduct(Integer productId) {
        User currentUser = getCurrentUserSafe();
        return reviewRepository.findByOrderDetailProductId(productId)
                .stream()
                .map(r -> toResponseWithReactions(r, currentUser))
                .toList();
    }

    @Override
    public List<ReviewResponse> getReviewsByMysteryBox(Integer mysteryBoxId) {
        User currentUser = getCurrentUserSafe();
        return reviewRepository.findByMysteryBoxId(mysteryBoxId)
                .stream()
                .map(r -> toResponseWithReactions(r, currentUser))
                .toList();
    }

    @Override
    public PageResponse<ReviewResponse> getReviewsByShopPaged(Integer shopId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<Review> pageResult = reviewRepository.findByShopOwnerId(shopId, pageable);
        User currentUser = getCurrentUserSafe();
        return PageResponse.<ReviewResponse>builder()
                .content(pageResult.getContent().stream()
                        .map(r -> toResponseWithReactions(r, currentUser))
                        .toList())
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

        User currentUser = getCurrentUser();
        if (!review.getShopOwner().getId().equals(currentUser.getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        review.setReplyFromShop(request.getReplyFromShop());
        return toResponseWithReactions(reviewRepository.save(review), currentUser);
    }

    @Override
    public ReviewShopReport getShopReport() {
        User shopOwner = getCurrentUser();
        return ReviewShopReport.builder()
                .averageRating(shopOwner.getRatingAverage())
                .fiveStarNumber(reviewRepository.countByShopOwnerAndRatingStar(shopOwner, BigDecimal.valueOf(5)))
                .fourStarNumber(reviewRepository.countByShopOwnerAndRatingStar(shopOwner, BigDecimal.valueOf(4)))
                .threeStarNumber(reviewRepository.countByShopOwnerAndRatingStar(shopOwner, BigDecimal.valueOf(3)))
                .twoStarNumber(reviewRepository.countByShopOwnerAndRatingStar(shopOwner, BigDecimal.valueOf(2)))
                .oneStarNumber(reviewRepository.countByShopOwnerAndRatingStar(shopOwner, BigDecimal.valueOf(1)))
                .totalReviewers(reviewRepository.countByShopOwner(shopOwner))
                .build();
    }

    @Override
    public List<ReviewResponse> getMyReviews() {
        User buyer = getCurrentUserSafe();
        if (buyer == null) return List.of();
        return reviewRepository.findByBuyerId(buyer.getId())
                .stream()
                .map(r -> toResponseWithReactions(r, buyer))
                .toList();
    }

    @Override
    @Transactional
    public ReviewResponse reactToReview(Integer reviewId, ReviewReactionRequest request) {

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new AppException(ErrorCode.REVIEW_NOT_FOUND));

        User currentUser = getCurrentUser();

        reviewReactionRepository.findByReviewAndUser(review, currentUser)
                .ifPresentOrElse(
                        existing -> {
                            if (existing.getReactionType() == request.getReactionType()) {
                                // Bấm lại cùng loại → xóa reaction (toggle off)
                                reviewReactionRepository.delete(existing);
                            } else {
                                // Đổi từ LIKE sang DISLIKE hoặc ngược lại
                                existing.setReactionType(request.getReactionType());
                                reviewReactionRepository.save(existing);
                            }
                        },
                        () -> {
                            // Chưa react → tạo mới
                            ReviewReaction reaction = ReviewReaction.builder()
                                    .review(review)
                                    .user(currentUser)
                                    .reactionType(request.getReactionType())
                                    .createdAt(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")))
                                    .build();
                            reviewReactionRepository.save(reaction);
                        }
                );

        return toResponseWithReactions(review, currentUser);
    }

    // ── Helper: map review + reaction counts ────────────────────────────────

    private ReviewResponse toResponseWithReactions(Review review, User currentUser) {
        ReviewResponse response = reviewMapper.toResponse(review);
        response.setLikeCount(
                reviewReactionRepository.countByReviewAndReactionType(review, ReactionType.LIKE));
        response.setDislikeCount(
                reviewReactionRepository.countByReviewAndReactionType(review, ReactionType.DISLIKE));
        if (currentUser != null) {
            reviewReactionRepository.findByReviewAndUser(review, currentUser)
                    .ifPresent(r -> response.setCurrentUserReaction(r.getReactionType().name()));
        }
        return response;
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }

    private User getCurrentUserSafe() {
        try {
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            if (email == null || email.equals("anonymousUser")) return null;
            return userRepository.findByEmail(email).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }
}
