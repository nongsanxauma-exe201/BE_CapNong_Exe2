package com.swd301.foodmarket.service.impl;

import com.swd301.foodmarket.dto.request.NotificationRequest;
import com.swd301.foodmarket.dto.response.NotificationResponse;
import com.swd301.foodmarket.entity.Notification;
import com.swd301.foodmarket.entity.User;
import com.swd301.foodmarket.entity.Role;
import com.swd301.foodmarket.entity.UserNotification;
import com.swd301.foodmarket.enums.RoleName;
import com.swd301.foodmarket.exception.AppException;
import com.swd301.foodmarket.exception.ErrorCode;
import com.swd301.foodmarket.mapper.NotificationMapper;
import com.swd301.foodmarket.repository.NotificationRepository;
import com.swd301.foodmarket.repository.UserNotificationRepository;
import com.swd301.foodmarket.repository.UserRepository;
import com.swd301.foodmarket.service.NotificationService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static lombok.AccessLevel.PRIVATE;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = PRIVATE, makeFinal = true)
public class NotificationServiceImpl implements NotificationService {

    NotificationRepository notificationRepository;
    UserNotificationRepository userNotificationRepository;
    UserRepository userRepository;
    NotificationMapper notificationMapper;

    // =====================================================================
    // ADMIN GỬI THÔNG BÁO CHO NHIỀU NHÓM
    // =====================================================================

    /**
     * ADMIN gửi thông báo cho 1 hoặc nhiều nhóm user (BUYER, SHOP_OWNER, SHIPPER)
     * Có thể gửi cho:
     * - Tất cả 3 nhóm: ["BUYER", "SHOP_OWNER", "SHIPPER"]
     * - 1 nhóm: ["BUYER"]
     * - 2 nhóm: ["BUYER", "SHIPPER"]
     */
    @Override
    public void createNotificationForGroups(NotificationRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        User admin = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        // Validate receiverTypes không rỗng
        if (request.getReceiverTypes() == null || request.getReceiverTypes().isEmpty()) {
            throw new AppException(ErrorCode.INVALID_NOTIFICATION_REQUEST);
        }

        // Validate role hợp lệ
        Set<String> validRoles = Set.of("BUYER", "SHOP_OWNER", "SHIPPER");
        List<RoleName> targetRoles = new java.util.ArrayList<>();
        for (String role : request.getReceiverTypes()) {
            if (!validRoles.contains(role)) {
                throw new AppException(ErrorCode.ROLE_NOT_FOUND);
            }
            targetRoles.add(RoleName.valueOf(role));
        }

        // Tạo receiverType string: "BUYER,SHOP_OWNER" hoặc "BUYER,SHIPPER,SHOP_OWNER"
        String receiverType = String.join(",", request.getReceiverTypes());

        Notification notification = Notification.builder()
                .admin(admin)
                .title(request.getTitle())
                .message(request.getMessage())
                .evidence(request.getEvidence())
                .receiverType(receiverType)
                .build();

        Notification savedNotification = notificationRepository.save(notification);

        // Tạo UserNotification cho tất cả user thuộc các role được chọn
        List<User> targetUsers = userRepository.findByRole_NameIn(targetRoles);
        List<UserNotification> userNotifications = targetUsers.stream()
                .map(user -> UserNotification.builder()
                        .user(user)
                        .notification(savedNotification)
                        .isRead(false)
                        .isDeleted(false)
                        .build())
                .collect(Collectors.toList());
        
        userNotificationRepository.saveAll(userNotifications);
    }

    // =====================================================================
    // XEM THÔNG BÁO
    // =====================================================================

    /**
     * User (BUYER/SHOP_OWNER/SHIPPER) xem tất cả thông báo của mình
     * Chỉ hiển thị notification mà admin gửi cho nhóm của user
     */
    @Override
    public List<NotificationResponse> getMyNotifications() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        String currentRole = getCurrentUserRole();

        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        // Lấy thông báo từ bảng UserNotification để có isRead và isDeleted của TỪNG user
        List<UserNotification> userNotifications = userNotificationRepository
                .findByUserAndIsDeletedFalseOrderByNotificationCreateAtDesc(currentUser);

        return userNotifications.stream()
                .map(un -> {
                    NotificationResponse response = notificationMapper.toResponse(un.getNotification());
                    response.setId(un.getId()); // FE cần ID của UserNotification để markRead/delete
                    response.setIsRead(un.getIsRead());
                    return response;
                })
                .collect(Collectors.toList());
    }

    /**
     * ADMIN xem tất cả thông báo hệ thống đã gửi
     */
    @Override
    public List<NotificationResponse> getAllNotifications() {
        List<Notification> notifications = notificationRepository.findAllByOrderByCreateAtDesc();

        return notifications.stream()
                .map(notificationMapper::toResponse)
                .collect(Collectors.toList());
    }
    @Override
    public void markAsRead(Integer id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        UserNotification un = userNotificationRepository.findByIdAndUser(id, currentUser)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED)); // Or NOT_FOUND

        un.setIsRead(true);
        userNotificationRepository.save(un);
    }

    @Override
    public void deleteNotification(Integer id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        UserNotification un = userNotificationRepository.findByIdAndUser(id, currentUser)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED)); // Or NOT_FOUND

        un.setIsDeleted(true);
        userNotificationRepository.save(un);
    }

    /**
     * Lấy role hiện tại của user đang login
     * @return BUYER, SHOP_OWNER, hoặc SHIPPER
     */
    private String getCurrentUserRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(role -> role.startsWith("ROLE_"))
                .map(role -> role.replace("ROLE_", ""))
                .findFirst()
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHORIZED));
    }
}