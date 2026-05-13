package com.swd301.foodmarket.service;

import com.swd301.foodmarket.dto.request.ChatMessageRequest;
import com.swd301.foodmarket.dto.response.ChatMessageResponse;
import com.swd301.foodmarket.dto.response.ChatOrderResponse;
import com.swd301.foodmarket.dto.response.ConversationResponse;
import com.swd301.foodmarket.entity.User;

import java.util.List;

public interface ChatService {

    /**
     * Gửi tin nhắn — được gọi từ WebSocket Controller.
     * @param sender  User lấy từ Principal trong WS session (đã qua JWT interceptor)
     * @param request { receiverId, content }
     */
    ChatMessageResponse sendMessage(User sender, ChatMessageRequest request);

    /**
     * Lấy lịch sử chat giữa 2 user — REST API.
     * @param currentUser  User đang login
     * @param otherUserId  ID người còn lại
     */
    List<ChatMessageResponse> getChatHistory(User currentUser, Integer otherUserId);

    /**
     * Lấy danh sách tất cả cuộc hội thoại của user hiện tại (inbox).
     */
    List<ConversationResponse> getMyConversations(User currentUser);

    /**
     * Đánh dấu đã đọc toàn bộ tin nhắn trong conversation với otherUser.
     */
    void markAsRead(User currentUser, Integer otherUserId);

    List<ChatOrderResponse> getOrdersInChat(User currentUser, Integer otherUserId);
}