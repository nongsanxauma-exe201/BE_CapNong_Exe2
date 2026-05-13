package com.swd301.foodmarket.controller;

import com.swd301.foodmarket.dto.request.NotificationRequest;
import com.swd301.foodmarket.dto.response.ApiResponse;
import com.swd301.foodmarket.dto.response.NotificationResponse;
import com.swd301.foodmarket.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static lombok.AccessLevel.PRIVATE;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@FieldDefaults(level = PRIVATE, makeFinal = true)
public class NotificationController {

    NotificationService notificationService;

    // =====================================================================
    // ADMIN GỬI THÔNG BÁO CHO NHIỀU NHÓM
    // =====================================================================
    @PostMapping("/admin/send-to-groups")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> adminSendToGroups(@RequestBody NotificationRequest request) {
        notificationService.createNotificationForGroups(request);
        return ApiResponse.<Void>builder()
                .message("Gửi thông báo thành công đến các nhóm: " + String.join(", ", request.getReceiverTypes()))
                .build();
    }
    // =====================================================================
    // XEM THÔNG BÁO
    // =====================================================================

    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('BUYER', 'SHOP_OWNER', 'SHIPPER')")
    public ApiResponse<List<NotificationResponse>> getMyNotifications() {
        List<NotificationResponse> notifications = notificationService.getMyNotifications();
        return ApiResponse.<List<NotificationResponse>>builder()
                .result(notifications)
                .message("Lấy danh sách thông báo thành công.")
                .build();
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<NotificationResponse>> getAllNotifications() {
        List<NotificationResponse> notifications = notificationService.getAllNotifications();
        return ApiResponse.<List<NotificationResponse>>builder()
                .result(notifications)
                .message("Lấy tất cả thông báo thành công.")
                .build();
    }

    // =====================================================================
    // THAO TÁC TRÊN THÔNG BÁO (TỪNG USER)
    // =====================================================================

    @PutMapping("/{id}/read")
    @PreAuthorize("hasAnyRole('BUYER', 'SHOP_OWNER', 'SHIPPER')")
    public ApiResponse<Void> markAsRead(@PathVariable Integer id) {
        notificationService.markAsRead(id);
        return ApiResponse.<Void>builder()
                .message("Đã đánh dấu đọc thông báo.")
                .build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('BUYER', 'SHOP_OWNER', 'SHIPPER')")
    public ApiResponse<Void> deleteNotification(@PathVariable Integer id) {
        notificationService.deleteNotification(id);
        return ApiResponse.<Void>builder()
                .message("Đã xóa thông báo.")
                .build();
    }
}