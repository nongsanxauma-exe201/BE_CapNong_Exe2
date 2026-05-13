package com.swd301.foodmarket.enums;

public enum WithdrawRequestStatus {
    PENDING,        // Shop tạo yêu cầu
    APPROVED,       // Admin duyệt
    PROCESSING,     // Đã chuyển cho kế toán xử lý
    SUCCESS,        // Kế toán báo đã chuyển tiền
    FAILED,         // Chuyển tiền lỗi
    REJECTED        // Admin từ chối
}
