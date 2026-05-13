package com.swd301.foodmarket.service;

import com.swd301.foodmarket.dto.request.AddCartItemRequest;
import com.swd301.foodmarket.dto.response.CartResponse;

public interface CartService {

    CartResponse addToCart(AddCartItemRequest request);

    CartResponse getCart();

    // Sản phẩm thường
    CartResponse updateQuantity(Integer productId, Integer quantity);
    void removeItem(Integer productId);

    // Túi mù (mới)
    CartResponse updateMysteryBoxQuantity(Integer mysteryBoxId, Integer quantity);

    void removeMysteryBox(Integer mysteryBoxId);

    void clearCart();

    CartResponse updateBuildComboQuantity(Integer comboId, Integer quantity);
    void removeBuildCombo(Integer comboId);

    CartResponse addBuildPlanToCart(Integer planId);
}