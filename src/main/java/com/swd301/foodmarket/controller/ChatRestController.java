package com.swd301.foodmarket.controller;

import com.swd301.foodmarket.dto.response.ApiResponse;
import com.swd301.foodmarket.dto.response.ChatMessageResponse;
import com.swd301.foodmarket.dto.response.ChatOrderResponse;
import com.swd301.foodmarket.dto.response.ConversationResponse;
import com.swd301.foodmarket.entity.User;
import com.swd301.foodmarket.exception.AppException;
import com.swd301.foodmarket.exception.ErrorCode;
import com.swd301.foodmarket.repository.UserRepository;
import com.swd301.foodmarket.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API cho chat (dùng để load dữ liệu ban đầu trước khi WS kết nối).
 *
 * GET  /chat/conversations         → inbox: danh sách tất cả cuộc hội thoại
 * GET  /chat/history/{otherUserId} → lịch sử chat với 1 user cụ thể
 * PATCH /chat/read/{otherUserId}   → đánh dấu đã đọc
 *
 * Tất cả endpoint đều cần JWT (anyRequest().authenticated() trong SecurityConfig)
 */
@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatRestController {

    private final ChatService chatService;
    private final UserRepository userRepository;

    /**
     * Lấy danh sách tất cả cuộc hội thoại của user hiện tại.
     * Dùng để render inbox / sidebar chat.
     */
    @GetMapping("/conversations")
    public ApiResponse<List<ConversationResponse>> getMyConversations() {
        User currentUser = getCurrentUser();
        log.info("GET /chat/conversations - user: {}", currentUser.getId());
        return ApiResponse.<List<ConversationResponse>>builder()
                .result(chatService.getMyConversations(currentUser))
                .build();
    }

    /**
     * Lấy lịch sử chat với 1 user.
     * VD: GET /chat/history/5 → toàn bộ tin nhắn giữa mình và user ID=5, sắp xếp cũ→mới
     */
    @GetMapping("/history/{otherUserId}")
    public ApiResponse<List<ChatMessageResponse>> getChatHistory(@PathVariable Integer otherUserId) {
        User currentUser = getCurrentUser();
        log.info("GET /chat/history/{} - user: {}", otherUserId, currentUser.getId());
        return ApiResponse.<List<ChatMessageResponse>>builder()
                .result(chatService.getChatHistory(currentUser, otherUserId))
                .build();
    }

    /**
     * Đánh dấu đã đọc toàn bộ tin nhắn trong conversation với otherUser.
     * Gọi khi user mở cửa sổ chat.
     */
    @PatchMapping("/read/{otherUserId}")
    public ApiResponse<Void> markAsRead(@PathVariable Integer otherUserId) {
        User currentUser = getCurrentUser();
        chatService.markAsRead(currentUser, otherUserId);
        return ApiResponse.<Void>builder()
                .message("Marked as read successfully")
                .build();
    }

    @GetMapping("/orders/{otherUserId}")
    public ApiResponse<List<ChatOrderResponse>> getOrdersInChat(@PathVariable Integer otherUserId) {
        User currentUser = getCurrentUser();
        log.info("GET /chat/orders/{} - user: {}", otherUserId, currentUser.getId());
        return ApiResponse.<List<ChatOrderResponse>>builder()
                .result(chatService.getOrdersInChat(currentUser, otherUserId))
                .build();
    }

    // ==================== HELPER ====================

    /**
     * Lấy User hiện tại từ SecurityContext (JWT đã được Spring parse tự động).
     * Giống pattern đang dùng trong UserServiceImpl.getMyInfo()
     */
    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }
}