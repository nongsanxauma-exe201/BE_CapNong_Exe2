package com.swd301.foodmarket.service.impl;

import com.swd301.foodmarket.ai.util.AiRouteResolver;
import com.swd301.foodmarket.dto.response.*;
import com.swd301.foodmarket.entity.Order;
import com.swd301.foodmarket.entity.User;
import com.swd301.foodmarket.enums.AiAccountActionType;
import com.swd301.foodmarket.enums.AiItemType;
import com.swd301.foodmarket.enums.RoleName;
import com.swd301.foodmarket.exception.AppException;
import com.swd301.foodmarket.repository.OrderRepository;
import com.swd301.foodmarket.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiAccountActionServiceImpl implements AiAccountActionService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final int ORDER_PREVIEW_LIMIT = 5;

    private final UserService userService;
    private final OrderService orderService;
    private final CartService cartService;
    private final WalletService walletService;
    private final OrderRepository orderRepository;

    @Override
    public AiChatResponse execute(User user, AiAccountActionType action, Integer orderId) {
        if (user == null) {
            return AiChatResponse.error("Bạn cần đăng nhập để thực hiện thao tác này.");
        }

        if (action == null) {
            return AiChatResponse.text("Mình chưa xác định được thao tác tài khoản bạn muốn thực hiện.");
        }

        RoleName role = resolveRole(user);
        if (!AiRouteResolver.isAllowedForRole(action, role)) {
            return AiChatResponse.error("Tài khoản của bạn không có quyền thực hiện thao tác này.");
        }

        if (orderId != null && action == AiAccountActionType.VIEW_ORDERS) {
            action = AiAccountActionType.VIEW_ORDER_DETAIL;
        }

        return switch (action) {
            case VIEW_PROFILE -> viewProfile(user, role);
            case EDIT_PROFILE -> editProfile(role);
            case VIEW_ORDERS -> viewOrders(user, role);
            case VIEW_ORDER_DETAIL -> viewOrderDetail(user, role, orderId);
            case VIEW_CART -> viewCart(role);
            case VIEW_WALLET -> viewWallet(user, role);
            case VIEW_RETURNS -> viewReturns(role);
        };
    }

    private AiChatResponse viewProfile(User user, RoleName role) {
        UserResponse profile = userService.getMyInfo();

        StringBuilder message = new StringBuilder();
        message.append("**Thông tin tài khoản của bạn:**\n\n");
        message.append("- **Họ tên:** ").append(nullToDash(profile.getFullName())).append('\n');
        message.append("- **Email:** ").append(nullToDash(profile.getEmail())).append('\n');
        message.append("- **SĐT:** ").append(nullToDash(profile.getPhoneNumber())).append('\n');
        message.append("- **Địa chỉ:** ").append(nullToDash(profile.getAddress())).append('\n');

        if (role == RoleName.SHOP_OWNER) {
            message.append("- **Tên shop:** ").append(nullToDash(profile.getShopName())).append('\n');
            message.append("- **Ngân hàng:** ").append(nullToDash(profile.getBankName())).append('\n');
            message.append("- **STK:** ").append(maskBankAccount(profile.getBankAccount())).append('\n');
        }

        return navigationResponse(
                message.toString(),
                AiRouteResolver.resolve(AiAccountActionType.VIEW_PROFILE, role),
                AiRouteResolver.actionLabel(AiAccountActionType.VIEW_PROFILE)
        );
    }

    private AiChatResponse editProfile(RoleName role) {
        return navigationResponse(
                """
                Để **cập nhật thông tin cá nhân**, bạn vui lòng vào trang hồ sơ và chỉnh sửa trực tiếp trên form.
                Mình không thay đổi thông tin nhạy cảm qua chat để bảo vệ tài khoản của bạn.
                """,
                AiRouteResolver.resolve(AiAccountActionType.EDIT_PROFILE, role),
                AiRouteResolver.actionLabel(AiAccountActionType.EDIT_PROFILE)
        );
    }

    private AiChatResponse viewOrders(User user, RoleName role) {
        List<OrderResponse> orders = fetchRecentOrders(user, role);

        if (orders.isEmpty()) {
            return navigationResponse(
                    "Bạn chưa có đơn hàng nào.",
                    AiRouteResolver.resolve(AiAccountActionType.VIEW_ORDERS, role),
                    AiRouteResolver.actionLabel(AiAccountActionType.VIEW_ORDERS)
            );
        }

        StringBuilder message = new StringBuilder("**Đơn hàng gần đây của bạn:**\n\n");
        int index = 1;
        for (OrderResponse order : orders) {
            message.append("**").append(index++).append(". Đơn #").append(order.getId()).append("**\n");
            message.append("- **Trạng thái:** ").append(nullToDash(order.getStatus())).append('\n');
            message.append("- **Tổng tiền:** ").append(formatMoney(order.getTotalAmount())).append('\n');
            if (order.getCreatedAt() != null) {
                message.append("- **Ngày đặt:** ").append(order.getCreatedAt().format(DATE_FMT)).append('\n');
            }
            if (order.getRecipientName() != null && !order.getRecipientName().isBlank()) {
                message.append("- **Người nhận:** ").append(order.getRecipientName());
                if (order.getRecipientPhone() != null && !order.getRecipientPhone().isBlank()) {
                    message.append(" — ").append(order.getRecipientPhone());
                }
                message.append('\n');
            }
            if (order.getShippingAddress() != null && !order.getShippingAddress().isBlank()) {
                message.append("- **Địa chỉ giao:** ").append(order.getShippingAddress()).append('\n');
            }
            if (order.getPaymentMethod() != null && !order.getPaymentMethod().isBlank()) {
                message.append("- **Thanh toán:** ").append(order.getPaymentMethod()).append('\n');
            }
            String itemsText = formatOrderItemsDetailed(order);
            if (!itemsText.isBlank()) {
                message.append("- **Sản phẩm:**\n").append(itemsText);
            }
            message.append('\n');
        }

        if (orders.size() >= ORDER_PREVIEW_LIMIT) {
            message.append("\n*Hiển thị ").append(ORDER_PREVIEW_LIMIT).append(" đơn mới nhất.*");
        }

        return navigationResponse(
                message.toString(),
                AiRouteResolver.resolve(AiAccountActionType.VIEW_ORDERS, role),
                AiRouteResolver.actionLabel(AiAccountActionType.VIEW_ORDERS)
        );
    }

    private AiChatResponse viewOrderDetail(User user, RoleName role, Integer orderId) {
        if (orderId == null) {
            return AiChatResponse.text("Bạn muốn xem đơn hàng số mấy? Ví dụ: \"xem đơn hàng 123\".");
        }

        Order order = orderRepository.findById(orderId)
                .orElse(null);

        if (order == null || !canAccessOrder(user, role, order)) {
            return AiChatResponse.error("Không tìm thấy đơn hàng hoặc bạn không có quyền xem đơn này.");
        }

        OrderResponse response = orderService.getOrderById(orderId);

        StringBuilder message = new StringBuilder();
        message.append("**Chi tiết đơn #").append(response.getId()).append("**\n\n");
        message.append("- **Trạng thái:** ").append(nullToDash(response.getStatus())).append('\n');
        message.append("- **Tổng tiền:** ").append(formatMoney(response.getTotalAmount())).append('\n');
        message.append("- **Phí ship:** ").append(formatMoney(response.getShippingFee())).append('\n');
        if (response.getCreatedAt() != null) {
            message.append("- **Ngày đặt:** ").append(response.getCreatedAt().format(DATE_FMT)).append('\n');
        }
        message.append("- **Người nhận:** ").append(nullToDash(response.getRecipientName()));
        if (response.getRecipientPhone() != null && !response.getRecipientPhone().isBlank()) {
            message.append(" — ").append(response.getRecipientPhone());
        }
        message.append('\n');
        message.append("- **Địa chỉ giao:** ").append(nullToDash(response.getShippingAddress())).append('\n');
        message.append("- **Thanh toán:** ").append(nullToDash(response.getPaymentMethod())).append('\n');
        if (response.getNote() != null && !response.getNote().isBlank()) {
            message.append("- **Ghi chú:** ").append(response.getNote()).append('\n');
        }
        message.append("\n**Danh sách món:**\n");
        message.append(formatOrderItemsDetailed(response));

        return navigationResponse(
                message.toString(),
                AiRouteResolver.orderDetailPath(role, orderId),
                "Xem chi tiết đơn #" + orderId
        );
    }

    private AiChatResponse viewCart(RoleName role) {
        CartResponse cart = cartService.getCart();
        List<CartItemResponse> items = cart.getItems() == null ? List.of() : cart.getItems();

        if (items.isEmpty()) {
            return navigationResponse(
                    "Giỏ hàng của bạn đang trống.",
                    AiRouteResolver.resolve(AiAccountActionType.VIEW_CART, role),
                    AiRouteResolver.actionLabel(AiAccountActionType.VIEW_CART),
                    null
            );
        }

        StringBuilder message = new StringBuilder("**Chi tiết giỏ hàng của bạn:**\n\n");
        int index = 1;
        for (CartItemResponse item : items) {
            message.append("**").append(index++).append(". ")
                    .append(nullToDash(item.getProductName())).append("**\n");
            message.append("  - **Số lượng:** ").append(item.getQuantity() != null ? item.getQuantity() : 1).append('\n');
            BigDecimal effectiveUnitPrice = (item.getSalePrice() != null && item.getDiscountPercent() != null && item.getDiscountPercent() > 0)
                    ? item.getSalePrice() : item.getPrice();
            message.append("  - **Đơn giá:** ").append(formatMoney(effectiveUnitPrice)).append('\n');
            message.append("  - **Thành tiền:** ").append(formatMoney(calcLineTotal(item))).append('\n');
            if (item.getShopName() != null && !item.getShopName().isBlank()) {
                message.append("  - **Shop:** ").append(item.getShopName()).append('\n');
            }
            if (item.getItemType() != null && !item.getItemType().isBlank()) {
                message.append("  - **Loại:** ")
                        .append("MYSTERY_BOX".equals(item.getItemType()) ? "Túi mù" : "Sản phẩm")
                        .append('\n');
            }
            message.append('\n');
        }
        message.append("**Tổng tạm tính:** ").append(formatMoney(cart.getTotalAmount()));

        return navigationResponse(
                message.toString(),
                AiRouteResolver.resolve(AiAccountActionType.VIEW_CART, role),
                AiRouteResolver.actionLabel(AiAccountActionType.VIEW_CART),
                toCartPreview(cart)
        );
    }

    private AiChatResponse viewWallet(User user, RoleName role) {
        try {
            WalletResponse wallet = role == RoleName.SHIPPER
                    ? walletService.getMyShipperWallet()
                    : walletService.getMyWallet();

            String message = """
                    **Thông tin ví:**

                    - **Số dư khả dụng:** %s
                    - **Đang giữ:** %s
                    - **Trạng thái:** %s
                    """.formatted(
                    formatMoney(wallet.getTotalBalance()),
                    formatMoney(wallet.getFrozenBalance()),
                    wallet.getStatus() != null ? wallet.getStatus().name() : "—"
            );

            return navigationResponse(
                    message,
                    AiRouteResolver.resolve(AiAccountActionType.VIEW_WALLET, role),
                    AiRouteResolver.actionLabel(AiAccountActionType.VIEW_WALLET)
            );
        } catch (AppException ex) {
            log.warn("Wallet access failed for user {}: {}", user.getId(), ex.getMessage());
            return navigationResponse(
                    "Bạn chưa có ví hoặc chưa kích hoạt ví. Vui lòng mở trang ví để kiểm tra.",
                    AiRouteResolver.resolve(AiAccountActionType.VIEW_WALLET, role),
                    AiRouteResolver.actionLabel(AiAccountActionType.VIEW_WALLET)
            );
        }
    }

    private AiChatResponse viewReturns(RoleName role) {
        return navigationResponse(
                "Bạn có thể xem và quản lý yêu cầu trả hàng trên trang đơn trả hàng.",
                AiRouteResolver.resolve(AiAccountActionType.VIEW_RETURNS, role),
                AiRouteResolver.actionLabel(AiAccountActionType.VIEW_RETURNS)
        );
    }

    private List<OrderResponse> fetchRecentOrders(User user, RoleName role) {
        if (role == RoleName.SHOP_OWNER) {
            PageResponse<OrderResponse> page = orderService.getAllOrdersPaged(0, ORDER_PREVIEW_LIMIT);
            return page.getContent() != null ? page.getContent() : List.of();
        }

        PageResponse<OrderResponse> page = orderService.getOrdersByUserIdPaged(user.getId(), 0, ORDER_PREVIEW_LIMIT);
        return page.getContent() != null ? page.getContent() : List.of();
    }

    private boolean canAccessOrder(User user, RoleName role, Order order) {
        if (role == RoleName.BUYER) {
            return order.getBuyer() != null && order.getBuyer().getId().equals(user.getId());
        }
        if (role == RoleName.SHOP_OWNER) {
            return order.getShopOwner() != null && order.getShopOwner().getId().equals(user.getId());
        }
        return false;
    }

    private RoleName resolveRole(User user) {
        if (user.getRole() == null || user.getRole().getName() == null) {
            return RoleName.BUYER;
        }
        return user.getRole().getName();
    }

    private AiChatResponse navigationResponse(String message, String url, String label) {
        return navigationResponse(message, url, label, null);
    }

    private AiChatResponse navigationResponse(
            String message,
            String url,
            String label,
            AiCartPreviewResponse cart
    ) {
        AiChatResponse.AiChatResponseBuilder builder = AiChatResponse.builder()
                .type(com.swd301.foodmarket.enums.AiResponseType.NAVIGATION)
                .message(message)
                .navigation(AiNavigationResponse.builder()
                        .url(url)
                        .label(label)
                        .build());

        if (cart != null) {
            builder.cart(cart);
        }

        return builder.build();
    }

    private AiCartPreviewResponse toCartPreview(CartResponse cart) {
        if (cart == null || cart.getItems() == null) {
            return AiCartPreviewResponse.builder()
                    .items(List.of())
                    .totalAmount(BigDecimal.ZERO)
                    .build();
        }

        List<AiCartItemResponse> items = cart.getItems().stream()
                .map(this::toAiCartItem)
                .collect(Collectors.toList());

        return AiCartPreviewResponse.builder()
                .items(items)
                .totalAmount(cart.getTotalAmount())
                .build();
    }

    private AiCartItemResponse toAiCartItem(CartItemResponse item) {
        AiItemType itemType = "MYSTERY_BOX".equalsIgnoreCase(item.getItemType())
                ? AiItemType.MYSTERY_BOX
                : AiItemType.PRODUCT;

        Integer itemId = item.getMysteryBoxId() != null
                ? item.getMysteryBoxId()
                : item.getProductId();

        // Dùng salePrice nếu đang có giảm giá, ngược lại dùng price
        boolean hasDiscount = item.getSalePrice() != null
                && item.getDiscountPercent() != null
                && item.getDiscountPercent() > 0;
        BigDecimal effectivePrice = hasDiscount ? item.getSalePrice() : item.getPrice();

        return AiCartItemResponse.builder()
                .itemType(itemType)
                .itemId(itemId)
                .name(item.getProductName())
                .imageUrl(item.getImageUrl())
                .quantity(item.getQuantity() != null ? item.getQuantity().doubleValue() : 1.0)
                .unitPrice(effectivePrice)
                .originalPrice(hasDiscount ? item.getPrice() : null)
                .discountPercent(hasDiscount ? item.getDiscountPercent() : null)
                .totalPrice(calcLineTotal(item))
                .build();
    }

    private BigDecimal calcLineTotal(CartItemResponse item) {
        boolean hasDiscount = item.getSalePrice() != null
                && item.getDiscountPercent() != null
                && item.getDiscountPercent() > 0;
        BigDecimal effectivePrice = hasDiscount ? item.getSalePrice() : item.getPrice();
        if (effectivePrice == null) return null;
        int qty = item.getQuantity() != null ? item.getQuantity() : 1;
        return effectivePrice.multiply(BigDecimal.valueOf(qty));
    }

    private String formatOrderItemsDetailed(OrderResponse order) {
        if (order.getItems() == null || order.getItems().isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        int index = 1;
        for (OrderItemResponse item : order.getItems()) {
            sb.append("  ").append(index++).append(". **")
                    .append(nullToDash(item.getProductName())).append("** — ");
            sb.append(item.getQuantity() != null ? item.getQuantity() : 1);
            if (item.getUnit() != null && !item.getUnit().isBlank()) {
                sb.append(' ').append(item.getUnit());
            }
            sb.append(" × ").append(formatMoney(item.getUnitPrice()));
            if (item.getUnitPrice() != null && item.getQuantity() != null) {
                BigDecimal lineTotal = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                sb.append(" = **").append(formatMoney(lineTotal)).append("**");
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    private String nullToDash(String value) {
        return value == null || value.isBlank() ? "—" : value.trim();
    }

    private String formatMoney(BigDecimal amount) {
        if (amount == null) {
            return "—";
        }
        return String.format("%,.0fđ", amount);
    }

    private String maskBankAccount(String account) {
        if (account == null || account.isBlank()) {
            return "—";
        }
        String trimmed = account.trim();
        if (trimmed.length() <= 4) {
            return "****";
        }
        return "****" + trimmed.substring(trimmed.length() - 4);
    }
}
