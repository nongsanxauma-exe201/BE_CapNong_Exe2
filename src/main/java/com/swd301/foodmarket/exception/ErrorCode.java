package com.swd301.foodmarket.exception;


import lombok.Getter;
import org.springframework.http.HttpStatus;


@Getter
public enum ErrorCode {
    UNCATEGORIZED_EXCEPTION(9999,"Uncategorized Error", HttpStatus.INTERNAL_SERVER_ERROR),
    USER_EXISTED(1001, "User already exists", HttpStatus.BAD_REQUEST),
    ROLE_NOT_FOUND(1002, "Role not found", HttpStatus.NOT_FOUND),
    PRODUCT_NOT_FOUND(1003, "Product not found", HttpStatus.NOT_FOUND),
    CATEGORY_NOT_FOUND(1004, "Category not found", HttpStatus.NOT_FOUND),
    ORDER_NOT_FOUND(1005, "Order not found", HttpStatus.NOT_FOUND),
    INVALID_ORDER_STATUS(1006, "Invalid order status", HttpStatus.BAD_REQUEST),
    BUILD_COMBO_NOT_FOUND(1007, "Build combo not found", HttpStatus.NOT_FOUND),
    USER_DEACTIVATED(1008, "User account is deactivated", HttpStatus.FORBIDDEN),
    REVIEW_NOT_FOUND(1008, "Review not found", HttpStatus.NOT_FOUND),
    REVIEW_ALREADY_EXISTS(1008, "Review already exists for this order", HttpStatus.BAD_REQUEST),
    INVALID_QUANTITY(1008, "Invalid quantity", HttpStatus.BAD_REQUEST),
    INVALID_COMBO_PRICE(1009, "Invalid combo price", HttpStatus.BAD_REQUEST),
    INVALID_PRICE(1010, "Invalid price", HttpStatus.BAD_REQUEST),
    UNAUTHENTICATED(1003, "Unauthenticated", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(1004, "Unauthorized", HttpStatus.FORBIDDEN),
    INVALID_KEY(1005, "Invalid key", HttpStatus.BAD_REQUEST),
    CART_ITEM_NOT_FOUND(1006, "Cart item not found", HttpStatus.NOT_FOUND),
    PRODUCT_OUT_OF_STOCK(1007, "Product is out of stock", HttpStatus.BAD_REQUEST),
    USER_NOT_EXISTED(10010, "User not existed",HttpStatus.NOT_FOUND),
    MYSTERY_BOX_SOLD_OUT(10011, "Mystery box is sold out", HttpStatus.BAD_REQUEST),
//   Create user errors
    EMAIL_INVALID(1006, "Email invalid", HttpStatus.BAD_REQUEST),
    EMAIL_REQUIRED(1009, "Email required", HttpStatus.BAD_REQUEST),
    EMAIL_TOO_LONG(1011, "Email too long", HttpStatus.BAD_REQUEST),
    INVALID_PASSWORD(1007, "Password must be at least 8 characters", HttpStatus.BAD_REQUEST),
    PASSWORD_REQUIRED(1008, "Password required", HttpStatus.BAD_REQUEST),
    PASSWORD_WRONG(1013, "Password wrong", HttpStatus.BAD_REQUEST),
    ROLE_REQUIRED(1012, "Role required", HttpStatus.BAD_REQUEST),
    PASSWORD_MISMATCH(1014, "The verification password does not match", HttpStatus.BAD_REQUEST),
    PASSWORD_SAME_AS_OLD(1015, "The new password must not be the same as the old password", HttpStatus.BAD_REQUEST),

    MYSTERY_BOX_NOT_FOUND(2001, "Mystery box not found", HttpStatus.NOT_FOUND),

    NOTIFICATION_NOT_FOUND(1016, "Notification not found", HttpStatus.NOT_FOUND),
    INVALID_NOTIFICATION_REQUEST(1017, "Invalid request data", HttpStatus.BAD_REQUEST),
    MISS_ROLE(1018, "Misunderstand role", HttpStatus.BAD_REQUEST),

    USER_PENDING(1019, "User account is pending approval", HttpStatus.FORBIDDEN),
    PRODUCT_IN_USE(1020, "Product is in use and cannot be deleted", HttpStatus.BAD_REQUEST),
    // Wallet errors
    WALLET_EXISTED(3001, "Wallet already exists for this shop owner", HttpStatus.BAD_REQUEST),
    WALLET_NOT_FOUND(3002, "Wallet not found for this shop owner", HttpStatus.NOT_FOUND),
    WITHDRAW_REQUEST_NOT_FOUND(3003, "Withdraw request not found", HttpStatus.NOT_FOUND),
    INSUFFICIENT_BALANCE(3004, "Insufficient balance in wallet", HttpStatus.BAD_REQUEST),
    BANK_ACCOUNT_NOT_FOUND(3005, "Bank account not found for this user", HttpStatus.BAD_REQUEST),
    WITHDRAW_REQUEST_ALREADY_PROCESSED(3006, "Withdraw request has already been processed", HttpStatus.BAD_REQUEST),
    INVALID_AMOUNT(3007, "Invalid amount for withdraw request", HttpStatus.BAD_REQUEST),
    OUT_OF_STOCK(3008, "Product is out of stock", HttpStatus.BAD_REQUEST),
    PRODUCT_LIST_EMPTY(3009, "Product list cannot be empty", HttpStatus.BAD_REQUEST),
    MYSTERY_BOX_OUT_OF_STOCK(3010, "Mystery box is out of stock", HttpStatus.BAD_REQUEST),
    INVALID_REVIEW_TARGET(3011, "Invalid target for review", HttpStatus.BAD_REQUEST),
    MYSTERY_BOX_NOT_AVAILABLE(3012, "Mystery box is not available for purchase", HttpStatus.BAD_REQUEST),
    INVALID_WITHDRAW_STATUS(3008, "Invalid status for withdraw request", HttpStatus.BAD_REQUEST),
    PAYOS_CREATE_QR_FAILED(3009, "Failed to create QR code for PayOS", HttpStatus.INTERNAL_SERVER_ERROR),
    PLATFORM_WALLET_NOT_FOUND(3010, "Platform wallet not found", HttpStatus.INTERNAL_SERVER_ERROR),
    WALLET_FROZEN_BALANCE_NOT_ENOUGH(3011, "Frozen balance in wallet is not enough for this operation", HttpStatus.BAD_REQUEST),
    PLATFORM_BALANCE_NOT_ENOUGH(3012, "Platform balance is not enough for this operation", HttpStatus.INTERNAL_SERVER_ERROR),
    WALLET_BALANCE_NOT_ENOUGH(3013, "Wallet balance is not enough for this operation", HttpStatus.BAD_REQUEST),

    BLOG_NOT_FOUND(4001, "Blog not found", HttpStatus.NOT_FOUND),
    //PAYMENT
    PAYMENT_NOT_FOUND(5001, "Payment not found", HttpStatus.NOT_FOUND),
    ORDER_INVALID_STATUS(5002, "Order is in invalid status for this operation", HttpStatus.BAD_REQUEST),
    PAYOS_CREATE_FAILED(5003, "Failed to create PayOS payment", HttpStatus.INTERNAL_SERVER_ERROR),
    ORDER_NOT_PAYABLE(5004, "Order is not in a payable status", HttpStatus.BAD_REQUEST),
    INVALID_REQUEST(5005, "Invalid request data", HttpStatus.BAD_REQUEST),
    ORDER_ALREADY_TAKEN(4009, "Order already accepted by another shipper", HttpStatus.BAD_REQUEST),
    LOCATION_NOT_FOUND(4004, "Shipper location not found for this order", HttpStatus.NOT_FOUND),
    PAYMENT_ALREADY_SUCCESS(5006, "Payment has already been marked as successful", HttpStatus.BAD_REQUEST),

    //Build plan
    BUILD_PLAN_NOT_FOUND(6001, "Build plan not found", HttpStatus.NOT_FOUND),
    BUILD_PLAN_ITEM_NOT_FOUND(6002, "Build plan item not found", HttpStatus.NOT_FOUND),
    INVALID_BUILD_PLAN_ITEM(6003, "Invalid build plan item", HttpStatus.BAD_REQUEST),
    INVALID_INPUT(6004, "Invalid input data", HttpStatus.BAD_REQUEST),
    // Chatbot errors
    GEMINI_API_ERROR(7001, "Gemini API call failed", HttpStatus.INTERNAL_SERVER_ERROR),
    GEMINI_API_KEY_MISSING(7002, "Gemini API key is missing", HttpStatus.INTERNAL_SERVER_ERROR),
    GEMINI_RATE_LIMIT(7003, "Gemini API rate limit exceeded", HttpStatus.TOO_MANY_REQUESTS),
    BOT_KNOWLEDGE_NOT_FOUND(8001, "Bot knowledge not found", HttpStatus.NOT_FOUND),
    RETURN_REQUEST_NOT_FOUND(9001, "Return request not found", HttpStatus.NOT_FOUND),
    RETURN_REQUEST_ALREADY_EXISTS(9002, "Return request already exists for this item", HttpStatus.BAD_REQUEST),
    INVALID_RETURN_STATUS(9003, "Invalid status for this return operation", HttpStatus.BAD_REQUEST),
    ORDER_DETAIL_NOT_FOUND(9004, "Order detail not found", HttpStatus.NOT_FOUND),

    ;

    private final int code;
    private final String message;
    private final HttpStatus statusCode;

    ErrorCode(int code, String message, HttpStatus statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }
}
