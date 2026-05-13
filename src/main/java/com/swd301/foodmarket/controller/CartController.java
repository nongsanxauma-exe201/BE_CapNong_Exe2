package com.swd301.foodmarket.controller;

import com.swd301.foodmarket.dto.request.AddCartItemRequest;
import com.swd301.foodmarket.dto.response.ApiResponse;
import com.swd301.foodmarket.dto.response.CartResponse;
import com.swd301.foodmarket.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

        private final CartService cartService;

        @PostMapping("/add")
        public ApiResponse<CartResponse> addToCart(@RequestBody AddCartItemRequest request) {
                return ApiResponse.<CartResponse>builder()
                                .result(cartService.addToCart(request))
                                .build();
        }

        @GetMapping
        public ApiResponse<CartResponse> getCart() {
                return ApiResponse.<CartResponse>builder()
                                .result(cartService.getCart())
                                .build();
        }

        // Cập nhật số lượng sản phẩm thường
        @PutMapping("/quantity")
        public ApiResponse<CartResponse> updateQuantity(
                        @RequestParam(required = false) Integer productId,
                        @RequestParam(required = false) Integer mysteryBoxId,
                        @RequestParam Integer quantity) {
                return ApiResponse.<CartResponse>builder()
                                .result(cartService.updateQuantity(productId, quantity))
                                .build();
        }

        // Cập nhật số lượng túi mù
        @PutMapping("/mystery-box/quantity")
        public ApiResponse<CartResponse> updateMysteryBoxQuantity(
                        @RequestParam Integer mysteryBoxId,
                        @RequestParam Integer quantity) {
                return ApiResponse.<CartResponse>builder()
                                .result(cartService.updateMysteryBoxQuantity(mysteryBoxId, quantity))
                                .build();
        }

        // Xóa sản phẩm thường
        @DeleteMapping("/item/{productId}")
        public ApiResponse<String> removeItem(@PathVariable Integer productId) {
                cartService.removeItem(productId);
                return ApiResponse.<String>builder()
                                .result("Item removed from cart")
                                .build();
        }

        // Xóa túi mù
        @DeleteMapping("/mystery-box/{mysteryBoxId}")
        public ApiResponse<String> removeMysteryBox(@PathVariable Integer mysteryBoxId) {
                cartService.removeMysteryBox(mysteryBoxId);
                return ApiResponse.<String>builder()
                                .result("Mystery box removed from cart")
                                .build();
        }

        @PostMapping("/add-plan/{planId}")
        public ApiResponse<CartResponse> addPlanToCart(@PathVariable Integer planId) {
                return ApiResponse.<CartResponse>builder()
                                .result(cartService.addBuildPlanToCart(planId))
                                .build();
        }

        @DeleteMapping("/clear")
        public ApiResponse<String> clearCart() {
                cartService.clearCart();
                return ApiResponse.<String>builder()
                                .result("Cart cleared")
                                .build();
        }

        @PutMapping("/build-combo/quantity")
        public ApiResponse<CartResponse> updateBuildComboQuantity(
                        @RequestParam Integer comboId,
                        @RequestParam Integer quantity
        ) {
                return ApiResponse.<CartResponse>builder()
                                .result(cartService.updateBuildComboQuantity(comboId, quantity))
                                .build();
        }

        // Xóa combo
        @DeleteMapping("/build-combo/{comboId}")
        public ApiResponse<String> removeBuildCombo(@PathVariable Integer comboId) {
                cartService.removeBuildCombo(comboId);
                return ApiResponse.<String>builder()
                                .result("Build combo removed from cart")
                                .build();
        }
}