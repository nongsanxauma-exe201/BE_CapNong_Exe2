package com.swd301.foodmarket.service.impl;

import com.swd301.foodmarket.ai.exception.AiCartOperationException;
import com.swd301.foodmarket.ai.util.AiQuantityConverter;
import com.swd301.foodmarket.ai.util.AiQuantityConverter.ConversionResult;
import com.swd301.foodmarket.dto.request.AddCartItemRequest;
import com.swd301.foodmarket.dto.response.AiCartItemResponse;
import com.swd301.foodmarket.dto.response.AiCartPreviewResponse;
import com.swd301.foodmarket.dto.response.AiCheckoutDraftResponse;
import com.swd301.foodmarket.dto.response.CartResponse;
import com.swd301.foodmarket.entity.MysteryBox;
import com.swd301.foodmarket.entity.Product;
import com.swd301.foodmarket.entity.User;
import com.swd301.foodmarket.enums.AiItemType;
import com.swd301.foodmarket.service.AiCartGateway;
import com.swd301.foodmarket.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AiCartGatewayImpl implements AiCartGateway {

    private final CartService cartService;

    @Override
    public AiCartPreviewResponse addProductToCart(
            User user,
            Product product,
            Double quantity,
            String unit
    ) {
        ConversionResult conversion = AiQuantityConverter.resolveForProduct(product, quantity, unit);

        if (!conversion.isValid()) {
            throw new AiCartOperationException(conversion.getErrorMessage());
        }

        int cartQuantity = conversion.getCartQuantity();
        double displayQuantity = conversion.getDisplayQuantity();
        String displayUnit = conversion.getDisplayUnit();

        cartService.addToCart(
                AddCartItemRequest.builder()
                        .productId(product.getId())
                        .quantity(cartQuantity)
                        .build()
        );

        BigDecimal unitPrice = product.getSellingPrice() == null
                ? BigDecimal.ZERO
                : product.getSellingPrice();

        BigDecimal totalPrice = unitPrice.multiply(BigDecimal.valueOf(displayQuantity));

        AiCartItemResponse item = AiCartItemResponse.builder()
                .itemType(AiItemType.PRODUCT)
                .itemId(product.getId())
                .name(product.getProductName())
                .imageUrl(product.getImageUrl())
                .quantity(displayQuantity)
                .unit(displayUnit)
                .unitPrice(unitPrice)
                .totalPrice(totalPrice)
                .build();

        return AiCartPreviewResponse.builder()
                .items(List.of(item))
                .totalAmount(totalPrice)
                .note(conversion.getNote())
                .build();
    }

    @Override
    public AiCartPreviewResponse addMysteryBoxToCart(
            User user,
            MysteryBox mysteryBox,
            Integer quantity
    ) {
        ConversionResult conversion = AiQuantityConverter.resolveForMysteryBox(quantity);
        int finalQuantity = conversion.getCartQuantity();

        cartService.addToCart(
                AddCartItemRequest.builder()
                        .mysteryBoxId(mysteryBox.getId())
                        .quantity(finalQuantity)
                        .build()
        );

        BigDecimal unitPrice = mysteryBox.getPrice() == null
                ? BigDecimal.ZERO
                : mysteryBox.getPrice();

        BigDecimal totalPrice = unitPrice.multiply(BigDecimal.valueOf(finalQuantity));

        AiCartItemResponse item = AiCartItemResponse.builder()
                .itemType(AiItemType.MYSTERY_BOX)
                .itemId(mysteryBox.getId())
                .name(mysteryBox.getBoxType())
                .imageUrl(mysteryBox.getImageUrl())
                .quantity((double) finalQuantity)
                .unit("hộp")
                .unitPrice(unitPrice)
                .totalPrice(totalPrice)
                .build();

        return AiCartPreviewResponse.builder()
                .items(List.of(item))
                .totalAmount(totalPrice)
                .build();
    }

    @Override
    public AiCheckoutDraftResponse createCheckoutDraft(User user) {
        CartResponse cart = cartService.getCart();

        BigDecimal totalAmount = cart.getTotalAmount() == null
                ? BigDecimal.ZERO
                : cart.getTotalAmount();

        return AiCheckoutDraftResponse.builder()
                .checkoutUrl("/checkout")
                .totalAmount(totalAmount)
                .build();
    }
}
