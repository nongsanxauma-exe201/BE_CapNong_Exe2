package com.swd301.foodmarket.repository;

import com.swd301.foodmarket.entity.Review;
import com.swd301.foodmarket.entity.ReviewReaction;
import com.swd301.foodmarket.entity.User;
import com.swd301.foodmarket.enums.ReactionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReviewReactionRepository extends JpaRepository<ReviewReaction, Integer> {

    Optional<ReviewReaction> findByReviewAndUser(Review review, User user);

    long countByReviewAndReactionType(Review review, ReactionType reactionType);

    void deleteByReviewAndUser(Review review, User user);
}
