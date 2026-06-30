package com.swd301.foodmarket.exception;


import lombok.Getter;
import org.springframework.http.HttpStatus;


@Getter
public enum ErrorCode {
    UNCATEGORIZED_EXCEPTION(9999, "Uncategorized Error", HttpStatus.INTERNAL_SERVER_ERROR),
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
    USER_NOT_EXISTED(10010, "User not existed", HttpStatus.NOT_FOUND),
    MYSTERY_BOX_SOLD_OUT(10011, "Mystery box is sold out", HttpStatus.BAD_REQUEST),
    INVALID_LOGIN(10012, "Email or Password wrong", HttpStatus.BAD_REQUEST),
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
    TRIP_NOT_FOUND(2002, "Trip not found", HttpStatus.NOT_FOUND),
    TRIP_JOIN_REQUEST_NOT_FOUND(2003, "Trip join request not found", HttpStatus.NOT_FOUND),

    NOTIFICATION_NOT_FOUND(1016, "Notification not found", HttpStatus.NOT_FOUND),
    INVALID_NOTIFICATION_REQUEST(1017, "Invalid request data", HttpStatus.BAD_REQUEST),
    MISS_ROLE(1018, "Misunderstand role", HttpStatus.BAD_REQUEST),

    USER_PENDING(1019, "User account is pending approval", HttpStatus.FORBIDDEN),
    PRODUCT_IN_USE(1020, "Product is in use and cannot be deleted", HttpStatus.BAD_REQUEST),
    // Wallet errors
    WALLET_EXISTED(3001, "Ví đã tồn tại cho tài khoản này", HttpStatus.BAD_REQUEST),
    WALLET_NOT_FOUND(3002, "Không tìm thấy ví của tài khoản này", HttpStatus.NOT_FOUND),
    WITHDRAW_REQUEST_NOT_FOUND(3003, "Không tìm thấy yêu cầu rút tiền", HttpStatus.NOT_FOUND),
    INSUFFICIENT_BALANCE(3004, "Số dư ví không đủ để thực hiện thao tác này", HttpStatus.BAD_REQUEST),
    BANK_ACCOUNT_NOT_FOUND(3005, "Không tìm thấy tài khoản ngân hàng của người dùng này", HttpStatus.BAD_REQUEST),
    WITHDRAW_REQUEST_ALREADY_PROCESSED(3006, "Yêu cầu rút tiền đã được xử lý trước đó", HttpStatus.BAD_REQUEST),
    INVALID_AMOUNT(3007, "Số tiền yêu cầu không hợp lệ", HttpStatus.BAD_REQUEST),
    OUT_OF_STOCK(3008, "Product is out of stock", HttpStatus.BAD_REQUEST),
    PRODUCT_LIST_EMPTY(3009, "Product list cannot be empty", HttpStatus.BAD_REQUEST),
    MYSTERY_BOX_OUT_OF_STOCK(3010, "Mystery box is out of stock", HttpStatus.BAD_REQUEST),
    INVALID_REVIEW_TARGET(3011, "Invalid target for review", HttpStatus.BAD_REQUEST),
    MYSTERY_BOX_NOT_AVAILABLE(3012, "Mystery box is not available for purchase", HttpStatus.BAD_REQUEST),
    INVALID_WITHDRAW_STATUS(3008, "Trạng thái yêu cầu rút tiền không hợp lệ", HttpStatus.BAD_REQUEST),
    PAYOS_CREATE_QR_FAILED(3009, "Không thể tạo mã QR PayOS", HttpStatus.INTERNAL_SERVER_ERROR),
    PLATFORM_WALLET_NOT_FOUND(3010, "Không tìm thấy ví nền tảng", HttpStatus.INTERNAL_SERVER_ERROR),
    WALLET_FROZEN_BALANCE_NOT_ENOUGH(3011, "Số dư đang phong tỏa trong ví không đủ để thực hiện thao tác này", HttpStatus.BAD_REQUEST),
    PLATFORM_BALANCE_NOT_ENOUGH(3012, "Số dư ví nền tảng không đủ để thực hiện thao tác này", HttpStatus.INTERNAL_SERVER_ERROR),
    WALLET_BALANCE_NOT_ENOUGH(3013, "Số dư ví không đủ để thực hiện thao tác này", HttpStatus.BAD_REQUEST),
    INVALID_REFUND_AMOUNT(3014, "Số tiền hoàn không hợp lệ", HttpStatus.BAD_REQUEST),
    BLOG_NOT_FOUND(4001, "Blog not found", HttpStatus.NOT_FOUND),
    //PAYMENT
    PAYMENT_NOT_FOUND(5001, "Payment not found", HttpStatus.NOT_FOUND),
    ORDER_INVALID_STATUS(5002, "Order is in invalid status for this operation", HttpStatus.BAD_REQUEST),
    PAYOS_CREATE_FAILED(5003, "Failed to create PayOS payment", HttpStatus.INTERNAL_SERVER_ERROR),
    ORDER_NOT_PAYABLE(5004, "Order is not in a payable status", HttpStatus.BAD_REQUEST),
    INVALID_REQUEST(5005, "Invalid request data", HttpStatus.BAD_REQUEST),
    ORDER_ALREADY_TAKEN(4009, "Đơn hàng này đã được shipper khác nhận", HttpStatus.BAD_REQUEST),
    LOCATION_NOT_FOUND(4004, "Không tìm thấy vị trí shipper cho đơn hàng này", HttpStatus.NOT_FOUND),
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
    // Quality Check errors
    QUALITY_CRIERIA_NOT_FOUND(10001, "Quality criteria not found", HttpStatus.NOT_FOUND),
    QUALITY_CHECK_NOT_FOUND(10002, "Quality check not found", HttpStatus.NOT_FOUND),
    INVALID_QUALITY_CHECK_REQUEST(10003, "Invalid quality check request", HttpStatus.BAD_REQUEST),
    // Vie QR
    BANK_NOT_FOUND(11001, "bankBin is required", HttpStatus.BAD_REQUEST),
    ACCOUNT_NOT_FOUND(11002, "accountNo is required", HttpStatus.BAD_REQUEST),
    INVALID_MONEY(12001, "amount must be greater than 0", HttpStatus.BAD_REQUEST),

    // Product Gallery
    IMAGE_NOT_FOUND(12001, "Image not found", HttpStatus.NOT_FOUND),
    CANNOT_DELETE_PRIMARY_IMAGE(12002, "Cannot delete primary image. Set another image as primary first.", HttpStatus.BAD_REQUEST),
    MIN_IMAGES_REQUIRED(12003, "Product must have at least 1 image", HttpStatus.BAD_REQUEST),
    MAX_IMAGES_EXCEEDED(12004, "Product cannot have more than 8 images", HttpStatus.BAD_REQUEST),

    // Vehical
    VEHICLE_NOT_FOUND(13001, "Vehicle not found", HttpStatus.NOT_FOUND),
    VEHICLE_NUMBER_ALREADY_EXISTED(13002, "Vehicle number already existed", HttpStatus.BAD_REQUEST),
    //Warehouse
    WAREHOUSE_NOT_FOUND(14001, "Warehouse not found", HttpStatus.NOT_FOUND),
    // trip stop
    TRIP_STOP_NOT_FOUND(15001, "Trip stop not found", HttpStatus.NOT_FOUND),
    ///  cost
    INVALID_PAYMENT_METHOD(16001, "Invalid payment method", HttpStatus.BAD_REQUEST),
    COD_ALREADY_SETTLED(16002, "COD amount has already been settled for this order", HttpStatus.BAD_REQUEST),
    SHIPPER_NOT_FOUND(16003, "Shipper not found for this order", HttpStatus.BAD_REQUEST),

    // reprot user
    INVALID_TYPE(17001, "Invalid report type", HttpStatus.BAD_REQUEST),
    INVALID_DATE(17002, "from date must be before or equal to to date", HttpStatus.BAD_REQUEST),
    // VOUCHER
    VOUCHER_NOT_FOUND(18001, "Không tìm thấy voucher", HttpStatus.NOT_FOUND),
    VOUCHER_RECEIVE_LIMIT_REACHED(18002, "Voucher đã hết lượt nhận", HttpStatus.BAD_REQUEST),
    VOUCHER_ALREADY_CLAIMED(18003, "Bạn đã nhận voucher này rồi", HttpStatus.BAD_REQUEST),
    VOUCHER_USER_NOT_FOUND(18004, "Không tìm thấy voucher trong kho của bạn", HttpStatus.NOT_FOUND),
    VOUCHER_EXPIRED(18005, "Voucher đã hết hạn", HttpStatus.BAD_REQUEST),
    VOUCHER_INACTIVE(18006, "Voucher chưa được kích hoạt", HttpStatus.BAD_REQUEST)
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
