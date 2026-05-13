package com.swd301.foodmarket.service.impl;

import com.swd301.foodmarket.dto.request.AddCartItemRequest;
import com.swd301.foodmarket.dto.response.CartItemResponse;
import com.swd301.foodmarket.dto.response.CartResponse;
import com.swd301.foodmarket.entity.*;
import com.swd301.foodmarket.exception.AppException;
import com.swd301.foodmarket.exception.ErrorCode;
import com.swd301.foodmarket.repository.*;
import com.swd301.foodmarket.service.CartService;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final MysteryBoxRepository mysteryBoxRepository;
    private final UserRepository userRepository;
    private final BuildComboRepository buildComboRepository;
    private final BuildPlanRepository buildPlanRepository;

    private Cart getOrCreateCart(User user) {
        return cartRepository.findByUser(user)
                .orElseGet(() -> {
                    Cart newCart = new Cart();
                    newCart.setUser(user);
                    return cartRepository.save(newCart);
                });
    }

    @Transactional
    @Override
    public CartResponse addToCart(AddCartItemRequest request) {
        User user = getCurrentUser();
        Cart cart = getOrCreateCart(user);

        Integer count = 0;
        if (request.getProductId() != null) count++;
        if (request.getMysteryBoxId() != null) count++;
        if (request.getBuildComboId() != null) count++;

        if (count != 1) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        if (request.getMysteryBoxId() != null) {
            MysteryBox box = mysteryBoxRepository.findById(request.getMysteryBoxId())
                    .orElseThrow(() -> new AppException(ErrorCode.MYSTERY_BOX_NOT_FOUND));

            CartItem item = cartItemRepository.findByCartAndMysteryBox(cart, box).orElse(null);
            if (item != null) {
                item.setQuantity(item.getQuantity() + request.getQuantity());
            } else {
                item = new CartItem();
                item.setCart(cart);
                item.setMysteryBox(box);
                item.setProduct(null);
                item.setQuantity(request.getQuantity());
            }
            cartItemRepository.save(item);

        } else if (request.getProductId() != null) {
            Product product = productRepository.findById(request.getProductId())
                    .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

            CartItem item = cartItemRepository.findByCartAndProduct(cart, product).orElse(null);
            if (item != null) {
                item.setQuantity(item.getQuantity() + request.getQuantity());
            } else {
                item = new CartItem();
                item.setCart(cart);
                item.setProduct(product);
                item.setMysteryBox(null);
                item.setQuantity(request.getQuantity());
            }
            cartItemRepository.save(item);

        } else if (request.getBuildComboId() != null) {
            BuildCombo combo = buildComboRepository.findById(request.getBuildComboId())
                    .orElseThrow(() -> new AppException(ErrorCode.BUILD_COMBO_NOT_FOUND));

            CartItem item = cartItemRepository.findByCartAndBuildCombo(cart, combo).orElse(null);

            if (item != null) {
                item.setQuantity(item.getQuantity() + request.getQuantity());
            } else {
                item = new CartItem();
                item.setCart(cart);
                item.setBuildCombo(combo);
                item.setProduct(null);
                item.setMysteryBox(null);
                item.setQuantity(request.getQuantity());
            }

            cartItemRepository.save(item);
        }


        return getCart();
    }

    @Transactional
    @Override
    public CartResponse updateQuantity(Integer productId, Integer quantity) {
        User user = getCurrentUser();
        Cart cart = getOrCreateCart(user);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
        CartItem item = cartItemRepository.findByCartAndProduct(cart, product)
                .orElseThrow(() -> new AppException(ErrorCode.CART_ITEM_NOT_FOUND));
        if (quantity <= 0) cartItemRepository.delete(item);
        else { item.setQuantity(quantity); cartItemRepository.save(item); }
        return getCart();
    }

    @Transactional
    @Override
    public CartResponse updateMysteryBoxQuantity(Integer mysteryBoxId, Integer quantity) {
        User user = getCurrentUser();
        Cart cart = getOrCreateCart(user);
        MysteryBox box = mysteryBoxRepository.findById(mysteryBoxId)
                .orElseThrow(() -> new AppException(ErrorCode.MYSTERY_BOX_NOT_FOUND));
        CartItem item = cartItemRepository.findByCartAndMysteryBox(cart, box)
                .orElseThrow(() -> new AppException(ErrorCode.CART_ITEM_NOT_FOUND));
        if (quantity <= 0) cartItemRepository.delete(item);
        else { item.setQuantity(quantity); cartItemRepository.save(item); }
        return getCart();
    }

    @Transactional
    @Override
    public void removeItem(Integer productId) {
        User user = getCurrentUser();
        Cart cart = getOrCreateCart(user);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
        CartItem item = cartItemRepository.findByCartAndProduct(cart, product)
                .orElseThrow(() -> new AppException(ErrorCode.CART_ITEM_NOT_FOUND));
        cartItemRepository.delete(item);
    }

    @Transactional
    @Override
    public void removeMysteryBox(Integer mysteryBoxId) {
        User user = getCurrentUser();
        Cart cart = getOrCreateCart(user);
        MysteryBox box = mysteryBoxRepository.findById(mysteryBoxId)
                .orElseThrow(() -> new AppException(ErrorCode.MYSTERY_BOX_NOT_FOUND));
        CartItem item = cartItemRepository.findByCartAndMysteryBox(cart, box)
                .orElseThrow(() -> new AppException(ErrorCode.CART_ITEM_NOT_FOUND));
        cartItemRepository.delete(item);
    }

    @Transactional
    @Override
    public void clearCart() {
        User user = getCurrentUser();
        Cart cart = getOrCreateCart(user);
        cartItemRepository.deleteAllByCart(cart);
    }

    @Transactional
    @Override
    public CartResponse getCart() {
        User user = getCurrentUser();
        Cart cart = getOrCreateCart(user);
        List<CartItem> items = cartItemRepository.findByCart(cart);

        // ✅ Build CartItemResponse thủ công để thêm shopOwnerId
        List<CartItemResponse> itemResponses = items.stream().map(item -> {
            CartItemResponse.CartItemResponseBuilder builder = CartItemResponse.builder()
                    .quantity(item.getQuantity());

            if (item.getMysteryBox() != null) {
                MysteryBox box = item.getMysteryBox();
                builder
                        .mysteryBoxId(box.getId())
                        .productId(null)
                        .productName(box.getBoxType() != null ? box.getBoxType() : "Túi mù")
                        .price(box.getPrice())
                        .imageUrl(box.getImageUrl())
                        .shopName(box.getShopOwner() != null ? box.getShopOwner().getShopName() : null)
                        .itemType("MYSTERY_BOX")
                        .shopOwnerId(box.getShopOwner() != null ? box.getShopOwner().getId() : null);

            } else if (item.getProduct() != null) {
                Product product = item.getProduct();
                builder
                        .productId(product.getId())
                        .mysteryBoxId(null)
                        .productName(product.getProductName())
                        .price(product.getSellingPrice())
                        .imageUrl(product.getImageUrl())
                        .shopName(product.getShopOwner() != null ? product.getShopOwner().getShopName() : null)
                        .itemType("PRODUCT")
                        // ✅ shopOwnerId
                        .shopOwnerId(product.getShopOwner() != null ? product.getShopOwner().getId() : null);
            }else if (item.getBuildCombo() != null) {
                BuildCombo combo = item.getBuildCombo();
                builder.buildComboId(combo.getId())
                        .productId(null)
                        .mysteryBoxId(null)
                        .productName(combo.getComboName())
                        .price(combo.getDiscountPrice())
                        .imageUrl(combo.getImageUrl())
                        .shopName(combo.getShopOwner() != null ? combo.getShopOwner().getShopName() : null)
                        .itemType("BUILD_COMBO")
                        .shopOwnerId(combo.getShopOwner() != null ? combo.getShopOwner().getId() : null);
            }

            return builder.build();
        }).collect(Collectors.toList());

        // Tính tổng tiền
        BigDecimal totalAmount = items.stream()
                .map(item -> {
                    if (item.getMysteryBox() != null && item.getMysteryBox().getPrice() != null) {
                        return item.getMysteryBox().getPrice()
                                .multiply(BigDecimal.valueOf(item.getQuantity()));
                    } else if (item.getProduct() != null && item.getProduct().getSellingPrice() != null) {
                        return item.getProduct().getSellingPrice()
                                .multiply(BigDecimal.valueOf(item.getQuantity()));
                    } else if (item.getBuildCombo() != null && item.getBuildCombo().getDiscountPrice() != null) {
                        return item.getBuildCombo().getDiscountPrice()
                                .multiply(BigDecimal.valueOf(item.getQuantity()));
                    }
                    return BigDecimal.ZERO;
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return CartResponse.builder()
                .cartId(cart.getId())
                .items(itemResponses)
                .totalAmount(totalAmount)
                .build();
    }

    @Transactional
    @Override
    public CartResponse updateBuildComboQuantity(Integer comboId, Integer quantity) {
        User user = getCurrentUser();
        Cart cart = getOrCreateCart(user);

        BuildCombo combo = buildComboRepository.findById(comboId)
                .orElseThrow(() -> new AppException(ErrorCode.BUILD_COMBO_NOT_FOUND));

        CartItem item = cartItemRepository.findByCartAndBuildCombo(cart, combo)
                .orElseThrow(() -> new AppException(ErrorCode.CART_ITEM_NOT_FOUND));

        if (quantity <= 0) cartItemRepository.delete(item);
        else {
            item.setQuantity(quantity);
            cartItemRepository.save(item);
        }

        return getCart();
    }

    @Transactional
    @Override
    public void removeBuildCombo(Integer comboId) {
        User user = getCurrentUser();
        Cart cart = getOrCreateCart(user);

        BuildCombo combo = buildComboRepository.findById(comboId)
                .orElseThrow(() -> new AppException(ErrorCode.BUILD_COMBO_NOT_FOUND));

        CartItem item = cartItemRepository.findByCartAndBuildCombo(cart, combo)
                .orElseThrow(() -> new AppException(ErrorCode.CART_ITEM_NOT_FOUND));

        cartItemRepository.delete(item);
    }
    @Override
    @Transactional
    public CartResponse addBuildPlanToCart(Integer planId) {
        User user = getCurrentUser();
        Cart cart = getOrCreateCart(user);

        // Clear existing cart to make it a "Buy Plan" primary action
        cartItemRepository.deleteAllByCart(cart);

        BuildPlan plan = buildPlanRepository.findById(planId)
                .orElseThrow(() -> new AppException(ErrorCode.BUILD_PLAN_NOT_FOUND));

        // Iterate through all days -> meals -> items (combos)
        for (PlanDay day : plan.getDays()) {
            for (Meal meal : day.getMeals()) {
                for (MealItem mealItem : meal.getItems()) {
                    BuildCombo combo = mealItem.getCombo();
                    if (combo != null) {
                        CartItem cartItem = cartItemRepository.findByCartAndBuildCombo(cart, combo)
                                .orElse(null);
                        
                        // Số lượng thực tế = số món * số người ăn
                        int actualQuantity = (mealItem.getQuantity() != null ? mealItem.getQuantity() : 1) 
                                           * (plan.getNumberOfPeople() != null ? plan.getNumberOfPeople() : 1);

                        // Nếu đã có trong cart (từ cùng plan hoặc trước đó), tăng số lượng
                        if (cartItem != null) {
                            cartItem.setQuantity(cartItem.getQuantity() + actualQuantity);
                        } else {
                            cartItem = new CartItem();
                            cartItem.setCart(cart);
                            cartItem.setBuildCombo(combo);
                            cartItem.setQuantity(actualQuantity);
                        }
                        cartItemRepository.save(cartItem);
                    }
                }
            }
        }

        return getCart();
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) throw new AppException(ErrorCode.UNAUTHORIZED);
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }
}