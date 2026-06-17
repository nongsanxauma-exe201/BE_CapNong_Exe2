package com.swd301.foodmarket.ai.util;

import com.swd301.foodmarket.enums.AiAccountActionType;
import com.swd301.foodmarket.enums.RoleName;

public final class AiRouteResolver {

    private AiRouteResolver() {
    }

    public static String resolve(AiAccountActionType action, RoleName role) {
        if (action == null) {
            return "/";
        }

        RoleName safeRole = role != null ? role : RoleName.BUYER;

        return switch (action) {
            case VIEW_PROFILE, EDIT_PROFILE -> profilePath(safeRole);
            case VIEW_ORDERS -> ordersPath(safeRole);
            case VIEW_ORDER_DETAIL -> ordersPath(safeRole);
            case VIEW_CART -> "/cart";
            case VIEW_WALLET -> walletPath(safeRole);
            case VIEW_RETURNS -> returnsPath(safeRole);
        };
    }

    public static String orderDetailPath(RoleName role, Integer orderId) {
        if (orderId == null) {
            return ordersPath(role);
        }

        RoleName safeRole = role != null ? role : RoleName.BUYER;

        if (safeRole == RoleName.SHOP_OWNER) {
            return "/farmer/order-prep/" + orderId;
        }

        return "/tracking/" + orderId;
    }

    public static String actionLabel(AiAccountActionType action) {
        return switch (action) {
            case VIEW_PROFILE -> "Xem hồ sơ";
            case EDIT_PROFILE -> "Cập nhật thông tin";
            case VIEW_ORDERS -> "Xem đơn hàng";
            case VIEW_ORDER_DETAIL -> "Xem chi tiết đơn";
            case VIEW_CART -> "Xem giỏ hàng";
            case VIEW_WALLET -> "Xem ví";
            case VIEW_RETURNS -> "Xem đơn trả hàng";
        };
    }

    private static String profilePath(RoleName role) {
        return switch (role) {
            case SHOP_OWNER -> "/farmer/profile";
            case STAFF -> "/staff/profile";
            case SHIPPER -> "/shipper";
            default -> "/profile";
        };
    }

    private static String ordersPath(RoleName role) {
        return switch (role) {
            case SHOP_OWNER -> "/farmer/orders";
            default -> "/my-orders";
        };
    }

    private static String walletPath(RoleName role) {
        return switch (role) {
            case SHOP_OWNER -> "/farmer/wallet";
            case SHIPPER -> "/shipper";
            default -> "/wallet";
        };
    }

    private static String returnsPath(RoleName role) {
        return switch (role) {
            case SHOP_OWNER -> "/farmer/farmer-disputes";
            default -> "/my-returns";
        };
    }

    public static boolean isAllowedForRole(AiAccountActionType action, RoleName role) {
        if (action == null || role == null) {
            return false;
        }

        return switch (action) {
            case VIEW_CART -> role == RoleName.BUYER;
            case VIEW_RETURNS -> role == RoleName.BUYER || role == RoleName.SHOP_OWNER;
            case VIEW_WALLET -> role == RoleName.BUYER
                    || role == RoleName.SHOP_OWNER
                    || role == RoleName.SHIPPER;
            case VIEW_PROFILE, EDIT_PROFILE, VIEW_ORDERS, VIEW_ORDER_DETAIL ->
                    role == RoleName.BUYER
                            || role == RoleName.SHOP_OWNER
                            || role == RoleName.SHIPPER
                            || role == RoleName.STAFF;
        };
    }
}
