package com.swd301.foodmarket.service;

import com.swd301.foodmarket.dto.response.AiCartPreviewResponse;
import com.swd301.foodmarket.dto.response.AiCheckoutDraftResponse;
import com.swd301.foodmarket.entity.MysteryBox;
import com.swd301.foodmarket.entity.Product;
import com.swd301.foodmarket.entity.User;

public interface AiCartGateway {

    AiCartPreviewResponse addProductToCart(
            User user,
            Product product,
            Double quantity,
            String unit
    );

    AiCartPreviewResponse addMysteryBoxToCart(
            User user,
            MysteryBox mysteryBox,
            Integer quantity
    );

    AiCheckoutDraftResponse createCheckoutDraft(User user);
}
