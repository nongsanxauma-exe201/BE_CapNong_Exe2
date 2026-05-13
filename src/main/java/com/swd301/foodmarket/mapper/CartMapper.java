package com.swd301.foodmarket.mapper;

import com.swd301.foodmarket.dto.response.CartItemResponse;
import com.swd301.foodmarket.dto.response.CartResponse;
import com.swd301.foodmarket.entity.Cart;
import com.swd301.foodmarket.entity.CartItem;
import com.swd301.foodmarket.entity.MysteryBox;
import com.swd301.foodmarket.entity.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface CartMapper {

    // Dùng default method để xử lý cả 2 loại item
    default CartItemResponse toItemResponse(CartItem item) {
        if (item == null) return null;

        MysteryBox box = item.getMysteryBox();
        Product product = item.getProduct();

        if (box != null) {
            // ── Túi mù ──────────────────────────────────────────────
            String shopName = (box.getShopOwner() != null)
                    ? box.getShopOwner().getFullName()   // hoặc getShopName() tùy field User của bạn
                    : "Nông Trại Khác";

            return CartItemResponse.builder()
                    .mysteryBoxId(box.getId())
                    .productId(null)
                    .productName(box.getBoxType())
                    .price(box.getPrice())
                    .imageUrl(box.getImageUrl())
                    .shopName(shopName)
                    .itemType("MYSTERY_BOX")
                    .quantity(item.getQuantity())
                    .build();

        } else if (product != null) {
            // ── Sản phẩm thường ─────────────────────────────────────
            String shopName = (product.getShopOwner() != null)
                    ? product.getShopOwner().getShopName()    // điều chỉnh theo field thực tế
                    : "Nông Trại Khác";

            return CartItemResponse.builder()
                    .productId(product.getId())
                    .mysteryBoxId(null)
                    .productName(product.getProductName())
                    .price(product.getSellingPrice())
                    .imageUrl(product.getImageUrl())
                    .shopName(shopName)
                    .itemType("PRODUCT")
                    .quantity(item.getQuantity())
                    .build();

        } else {
            // Fallback phòng ngừa data rác
            return CartItemResponse.builder()
                    .itemType("UNKNOWN")
                    .quantity(item.getQuantity())
                    .build();
        }
    }

    default List<CartItemResponse> toItemResponseList(List<CartItem> items) {
        if (items == null) return List.of();
        return items.stream()
                .map(this::toItemResponse)
                .collect(Collectors.toList());
    }

    @Mapping(target = "cartId", source = "id")
    @Mapping(target = "items", source = "items")
    CartResponse toResponse(Cart cart);
}