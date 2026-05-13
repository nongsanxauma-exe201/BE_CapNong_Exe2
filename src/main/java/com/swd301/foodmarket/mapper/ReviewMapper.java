package com.swd301.foodmarket.mapper;

import com.swd301.foodmarket.dto.request.ReviewCreateRequest;
import com.swd301.foodmarket.dto.response.ReviewResponse;
import com.swd301.foodmarket.entity.Review;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ReviewMapper {
    Review toReview(ReviewCreateRequest request);

    @Mapping(source = "buyer.id", target = "buyerId")
    @Mapping(source = "buyer.fullName", target = "fullName")
    @Mapping(source = "shopOwner.id", target = "shopOwnerId")
    @Mapping(source = "orderDetail.id", target = "orderDetailId")
    @Mapping(source = "mysteryBox.id", target = "mysteryBoxId")
    @Mapping(source = "orderDetail.product.id", target = "productId")
    @Mapping(source = "orderDetail.product.productName", target = "productName")
    ReviewResponse toResponse(Review review);
}
