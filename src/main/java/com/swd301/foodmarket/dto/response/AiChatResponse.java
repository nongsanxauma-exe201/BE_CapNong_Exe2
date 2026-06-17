package com.swd301.foodmarket.dto.response;

import com.swd301.foodmarket.enums.AiResponseType;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiChatResponse {

    private AiResponseType type;
    private String message;
    private List<AiProductOptionResponse> itemOptions;
    private AiProductOptionResponse selectedProduct;
    private AiCartPreviewResponse cart;
    private AiCheckoutDraftResponse checkout;

    public static AiChatResponse text(String message) {
        return AiChatResponse.builder()
                .type(AiResponseType.TEXT)
                .message(message)
                .build();
    }

    public static AiChatResponse error(String message) {
        return AiChatResponse.builder()
                .type(AiResponseType.ERROR)
                .message(message)
                .build();
    }

    public static AiChatResponse itemOptions(String message, List<AiProductOptionResponse> options) {
        return AiChatResponse.builder()
                .type(AiResponseType.ITEM_OPTIONS)
                .message(message)
                .itemOptions(options)
                .build();
    }

    public static AiChatResponse productDetail(String message, AiProductOptionResponse product) {
        return AiChatResponse.builder()
                .type(AiResponseType.PRODUCT_DETAIL)
                .message(message)
                .selectedProduct(product)
                .build();
    }

    public static AiChatResponse cartUpdated(String message, AiCartPreviewResponse cart) {
        return AiChatResponse.builder()
                .type(AiResponseType.CART_UPDATED)
                .message(message)
                .cart(cart)
                .build();
    }

    public static AiChatResponse checkoutDraft(String message, AiCheckoutDraftResponse checkout) {
        return AiChatResponse.builder()
                .type(AiResponseType.CHECKOUT_DRAFT)
                .message(message)
                .checkout(checkout)
                .build();
    }
}
