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
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
        BigDecimal requestedKg = resolveRequestKg(request.getQuantityKg(), request.getQuantity());
        Integer legacyQuantity = resolveLegacyQuantity(requestedKg, request.getQuantity());

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
                item.setQuantity(resolveLegacyQuantity(resolveCartItemKg(item), item.getQuantity()) + legacyQuantity);
                item.setQuantityKg(resolveCartItemKg(item).add(requestedKg));
            } else {
                item = new CartItem();
                item.setCart(cart);
                item.setMysteryBox(box);
                item.setProduct(null);
                item.setQuantity(legacyQuantity);
                item.setQuantityKg(requestedKg);
            }
            cartItemRepository.save(item);

        } else if (request.getProductId() != null) {
            Product product = productRepository.findById(request.getProductId())
                    .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

            CartItem item = cartItemRepository.findByCartAndProduct(cart, product).orElse(null);
            validateProductStockKg(product, requestedKg);
            if (item != null) {
                item.setQuantity(resolveLegacyQuantity(resolveCartItemKg(item), item.getQuantity()) + legacyQuantity);
                item.setQuantityKg(resolveCartItemKg(item).add(requestedKg));
                validateProductStockKg(product, item.getQuantityKg());
            } else {
                item = new CartItem();
                item.setCart(cart);
                item.setProduct(product);
                item.setMysteryBox(null);
                item.setQuantity(legacyQuantity);
                item.setQuantityKg(requestedKg);
            }
            cartItemRepository.save(item);

        } else if (request.getBuildComboId() != null) {
            BuildCombo combo = buildComboRepository.findById(request.getBuildComboId())
                    .orElseThrow(() -> new AppException(ErrorCode.BUILD_COMBO_NOT_FOUND));

            CartItem item = cartItemRepository.findByCartAndBuildCombo(cart, combo).orElse(null);

            if (item != null) {
                item.setQuantity(resolveLegacyQuantity(resolveCartItemKg(item), item.getQuantity()) + legacyQuantity);
                item.setQuantityKg(resolveCartItemKg(item).add(requestedKg));
            } else {
                item = new CartItem();
                item.setCart(cart);
                item.setBuildCombo(combo);
                item.setProduct(null);
                item.setMysteryBox(null);
                item.setQuantity(legacyQuantity);
                item.setQuantityKg(requestedKg);
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
        BigDecimal quantityKg = resolveRequestKg(null, quantity);
        if (quantity <= 0) cartItemRepository.delete(item);
        else {
            validateProductStockKg(product, quantityKg);
            item.setQuantity(quantity);
            item.setQuantityKg(quantityKg);
            cartItemRepository.save(item);
        }
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
                    .quantity(item.getQuantity())
                    .quantityKg(resolveCartItemKg(item));

            if (item.getMysteryBox() != null) {
                MysteryBox box = item.getMysteryBox();
                builder
                        .mysteryBoxId(box.getId())
                        .productId(null)
                        .productName(box.getBoxType() != null ? box.getBoxType() : "Túi mù")
                        .price(box.getPrice())
                        .pricePerKg(box.getPrice())
                        .itemTotal(multiplyMoney(box.getPrice(), resolveCartItemKg(item)))
                        .imageUrl(box.getImageUrl())
                        .shopName(box.getShopOwner() != null ? box.getShopOwner().getShopName() : null)
                        .itemType("MYSTERY_BOX")
                        .shopOwnerId(box.getShopOwner() != null ? box.getShopOwner().getId() : null)
                        .stockQuantity(box.getTotalQuantity() != null && box.getSoldQuantity() != null
                                ? box.getTotalQuantity() - box.getSoldQuantity() : null);

            } else if (item.getProduct() != null) {
                Product product = item.getProduct();
                BigDecimal sellingPrice = product.getSellingPrice();
                Integer pct = product.getDiscountPercent();
                BigDecimal salePrice = null;
                if (pct != null && pct > 0 && pct < 100 && sellingPrice != null) {
                    LocalDateTime now   = LocalDateTime.now();
                    LocalDateTime start = product.getDiscountStartDate();
                    LocalDateTime end   = product.getDiscountEndDate();
                    boolean active = (start == null || !now.isBefore(start))
                                  && (end   == null || !now.isAfter(end));
                    if (active) {
                        salePrice = sellingPrice.multiply(BigDecimal.valueOf(100 - pct))
                                .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
                    }
                }
                builder
                        .productId(product.getId())
                        .mysteryBoxId(null)
                        .productName(product.getProductName())
                        .price(sellingPrice)
                        .pricePerKg(sellingPrice)
                        .discountPercent(salePrice != null ? pct : null)
                        .salePrice(salePrice)
                        .itemTotal(multiplyMoney(salePrice != null ? salePrice : sellingPrice, resolveCartItemKg(item)))
                        .imageUrl(product.getImageUrl())
                        .shopName(product.getShopOwner() != null ? product.getShopOwner().getShopName() : null)
                        .itemType("PRODUCT")
                        .shopOwnerId(product.getShopOwner() != null ? product.getShopOwner().getId() : null)
                        .stockQuantity(product.getStockQuantity())
                        .stockKg(resolveProductStockKg(product));
            }else if (item.getBuildCombo() != null) {
                BuildCombo combo = item.getBuildCombo();
                builder.buildComboId(combo.getId())
                        .productId(null)
                        .mysteryBoxId(null)
                        .productName(combo.getComboName())
                        .price(combo.getDiscountPrice())
                        .pricePerKg(combo.getDiscountPrice())
                        .itemTotal(multiplyMoney(combo.getDiscountPrice(), resolveCartItemKg(item)))
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
                                .multiply(resolveCartItemKg(item));
                    } else if (item.getProduct() != null && item.getProduct().getSellingPrice() != null) {
                        return item.getProduct().getSellingPrice()
                                .multiply(resolveCartItemKg(item));
                    } else if (item.getBuildCombo() != null && item.getBuildCombo().getDiscountPrice() != null) {
                        return item.getBuildCombo().getDiscountPrice()
                                .multiply(resolveCartItemKg(item));
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

    @Override
    @Transactional
    public void restoreOrderItemsToCart(Order order) {
        if (order == null || order.getBuyer() == null) {
            return;
        }

        Cart cart = getOrCreateCart(order.getBuyer());

        if (order.getOrderDetails() != null && !order.getOrderDetails().isEmpty()) {
            Map<Integer, Integer> productQuantities = new HashMap<>();
            Map<Integer, BigDecimal> productQuantityKgs = new HashMap<>();
            for (OrderDetail detail : order.getOrderDetails()) {
                if (detail == null || detail.getProduct() == null || detail.getQuantity() == null || detail.getQuantity() <= 0) {
                    continue;
                }
                Integer productId = detail.getProduct().getId();
                if (productId == null) {
                    continue;
                }
                productQuantities.merge(productId, detail.getQuantity(), Integer::sum);
                productQuantityKgs.merge(productId, resolveOrderDetailKg(detail), BigDecimal::add);
            }

            for (Map.Entry<Integer, Integer> entry : productQuantities.entrySet()) {
                Product product = productRepository.findById(entry.getKey()).orElse(null);
                if (product == null) {
                    continue;
                }
                CartItem item = cartItemRepository.findByCartAndProduct(cart, product).orElse(null);
                BigDecimal quantityKg = productQuantityKgs.getOrDefault(entry.getKey(), BigDecimal.valueOf(entry.getValue()));
                if (item != null) {
                    item.setQuantity(Math.max(item.getQuantity(), entry.getValue()));
                    item.setQuantityKg(resolveCartItemKg(item).max(quantityKg));
                } else {
                    item = new CartItem();
                    item.setCart(cart);
                    item.setProduct(product);
                    item.setMysteryBox(null);
                    item.setBuildCombo(null);
                    item.setQuantity(entry.getValue());
                    item.setQuantityKg(quantityKg);
                }
                cartItemRepository.save(item);
            }
        }

        if (order.getMysteryBoxes() != null && !order.getMysteryBoxes().isEmpty()) {
            Map<Integer, Integer> boxQuantities = new HashMap<>();
            for (OrderMysteryBox orderBox : order.getMysteryBoxes()) {
                if (orderBox == null || orderBox.getMysteryBox() == null || orderBox.getQuantity() == null || orderBox.getQuantity() <= 0) {
                    continue;
                }
                Integer boxId = orderBox.getMysteryBox().getId();
                if (boxId == null) {
                    continue;
                }
                boxQuantities.merge(boxId, orderBox.getQuantity(), Integer::sum);
            }

            for (Map.Entry<Integer, Integer> entry : boxQuantities.entrySet()) {
                MysteryBox box = mysteryBoxRepository.findById(entry.getKey()).orElse(null);
                if (box == null) {
                    continue;
                }
                CartItem item = cartItemRepository.findByCartAndMysteryBox(cart, box).orElse(null);
                if (item != null) {
                    item.setQuantity(Math.max(item.getQuantity(), entry.getValue()));
                } else {
                    item = new CartItem();
                    item.setCart(cart);
                    item.setProduct(null);
                    item.setMysteryBox(box);
                    item.setBuildCombo(null);
                    item.setQuantity(entry.getValue());
                }
                cartItemRepository.save(item);
            }
        }

        if (order.getBuildCombos() != null && !order.getBuildCombos().isEmpty()) {
            Map<Integer, Integer> comboQuantities = new HashMap<>();
            for (BuildCombo combo : order.getBuildCombos()) {
                if (combo == null || combo.getId() == null) {
                    continue;
                }
                comboQuantities.merge(combo.getId(), 1, Integer::sum);
            }

            for (Map.Entry<Integer, Integer> entry : comboQuantities.entrySet()) {
                BuildCombo combo = buildComboRepository.findById(entry.getKey()).orElse(null);
                if (combo == null) {
                    continue;
                }
                CartItem item = cartItemRepository.findByCartAndBuildCombo(cart, combo).orElse(null);
                if (item != null) {
                    item.setQuantity(Math.max(item.getQuantity(), entry.getValue()));
                } else {
                    item = new CartItem();
                    item.setCart(cart);
                    item.setProduct(null);
                    item.setMysteryBox(null);
                    item.setBuildCombo(combo);
                    item.setQuantity(entry.getValue());
                }
                cartItemRepository.save(item);
            }
        }
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) throw new AppException(ErrorCode.UNAUTHORIZED);
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }

    private BigDecimal resolveRequestKg(BigDecimal quantityKg, Integer quantity) {
        BigDecimal resolved = quantityKg != null ? quantityKg : BigDecimal.valueOf(quantity == null ? 1 : quantity);
        return resolved.setScale(2, RoundingMode.HALF_UP);
    }

    private Integer resolveLegacyQuantity(BigDecimal quantityKg, Integer quantity) {
        if (quantity != null && quantity > 0) return quantity;
        return quantityKg == null ? 1 : Math.max(1, quantityKg.setScale(0, RoundingMode.CEILING).intValue());
    }

    private BigDecimal resolveCartItemKg(CartItem item) {
        if (item == null) return BigDecimal.ZERO;
        if (item.getQuantityKg() != null) return item.getQuantityKg();
        return BigDecimal.valueOf(item.getQuantity() == null ? 0 : item.getQuantity()).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal resolveOrderDetailKg(OrderDetail detail) {
        if (detail.getQuantityKg() != null) return detail.getQuantityKg();
        return BigDecimal.valueOf(detail.getQuantity() == null ? 0 : detail.getQuantity()).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal resolveProductStockKg(Product product) {
        if (product.getStockKg() != null) return product.getStockKg();
        return BigDecimal.valueOf(product.getStockQuantity() == null ? 0 : product.getStockQuantity()).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal multiplyMoney(BigDecimal pricePerKg, BigDecimal quantityKg) {
        if (pricePerKg == null || quantityKg == null) return BigDecimal.ZERO;
        return pricePerKg.multiply(quantityKg).setScale(0, RoundingMode.HALF_UP);
    }

    private void validateProductStockKg(Product product, BigDecimal requestedKg) {
        BigDecimal stockKg = resolveProductStockKg(product);
        if (stockKg.compareTo(requestedKg) < 0) {
            throw new RuntimeException("Sản phẩm chỉ còn " + stockKg.stripTrailingZeros().toPlainString()
                    + " kg trong kho, không đủ để mua " + requestedKg.stripTrailingZeros().toPlainString() + " kg.");
        }
    }
}