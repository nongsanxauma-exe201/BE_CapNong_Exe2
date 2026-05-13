package com.swd301.foodmarket.service.impl;

import com.swd301.foodmarket.dto.request.ChatMessageRequest;
import com.swd301.foodmarket.dto.response.ChatMessageResponse;
import com.swd301.foodmarket.dto.response.ChatOrderResponse;
import com.swd301.foodmarket.dto.response.ConversationResponse;
import com.swd301.foodmarket.entity.*;
import com.swd301.foodmarket.exception.AppException;
import com.swd301.foodmarket.exception.ErrorCode;
import com.swd301.foodmarket.repository.ChatMessageRepository;
import com.swd301.foodmarket.repository.ConversationRepository;
import com.swd301.foodmarket.repository.OrderRepository;
import com.swd301.foodmarket.repository.UserRepository;
import com.swd301.foodmarket.service.ChatService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatServiceImpl implements ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final ConversationRepository conversationRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    @Override
    @Transactional
    public ChatMessageResponse sendMessage(User sender, ChatMessageRequest request) {
        // 1. Tìm người nhận
        User receiver = userRepository.findById(request.getReceiverId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        // 2. Tạo roomKey chuẩn: luôn sort id tăng dần
        //    VD: sender=7, receiver=3 → "3_7" (không phải "7_3")
        String roomKey = buildRoomKey(sender.getId(), receiver.getId());

        // 3. Tìm conversation hiện có hoặc tạo mới
        Conversation conversation = conversationRepository.findByRoomKey(roomKey)
                .orElseGet(() -> {
                    User u1 = sender.getId() < receiver.getId() ? sender : receiver;
                    User u2 = sender.getId() < receiver.getId() ? receiver : sender;
                    return conversationRepository.save(
                            Conversation.builder()
                                    .roomKey(roomKey)
                                    .user1(u1)
                                    .user2(u2)
                                    .build()
                    );
                });

        // 4. Lưu tin nhắn
        ChatMessage message = ChatMessage.builder()
                .conversation(conversation)
                .sender(sender)
                .content(request.getContent())
                .build();
        ChatMessage saved = chatMessageRepository.save(message);

        // 5. Cập nhật lastMessageAt để inbox sort đúng
        conversation.setLastMessageAt(LocalDateTime.now());
        conversationRepository.save(conversation);

        log.info("Message saved | {} -> {} | content: {}", sender.getId(), receiver.getId(), request.getContent());

        return toResponse(saved);
    }

    @Override
    public List<ChatMessageResponse> getChatHistory(User currentUser, Integer otherUserId) {
        // Kiểm tra user kia có tồn tại không
        userRepository.findById(otherUserId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        String roomKey = buildRoomKey(currentUser.getId(), otherUserId);
        Conversation conversation = conversationRepository.findByRoomKey(roomKey)
                .orElse(null);

        if (conversation == null) {
            return List.of(); // Chưa từng nhắn tin → trả về rỗng
        }

        return chatMessageRepository.findByConversationOrderBySentAtAsc(conversation)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ConversationResponse> getMyConversations(User currentUser) {
        return conversationRepository.findAllByUser(currentUser)
                .stream()
                .map(conv -> {
                    // Xác định "người còn lại" trong conversation (không phải mình)
                    User other = conv.getUser1().getId().equals(currentUser.getId())
                            ? conv.getUser2()
                            : conv.getUser1();

                    // Lấy tin nhắn cuối
                    List<ChatMessage> messages =
                            chatMessageRepository.findByConversationOrderBySentAtAsc(conv);
                    String lastMsg = messages.isEmpty()
                            ? ""
                            : messages.get(messages.size() - 1).getContent();

                    int unread = chatMessageRepository.countUnread(conv, currentUser);

                    return ConversationResponse.builder()
                            .id(conv.getId())
                            .roomKey(conv.getRoomKey())
                            .otherUserId(other.getId())
                            .otherUserName(other.getFullName())
                            .otherUserRole(other.getRole().getName().name())
                            .lastMessage(lastMsg)
                            .lastMessageAt(conv.getLastMessageAt())
                            .unreadCount(unread)
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void markAsRead(User currentUser, Integer otherUserId) {
        String roomKey = buildRoomKey(currentUser.getId(), otherUserId);
        conversationRepository.findByRoomKey(roomKey).ifPresent(conv ->
                chatMessageRepository.markAllAsRead(conv, currentUser)
        );
    }

    @Override
    @Transactional
    public List<ChatOrderResponse> getOrdersInChat(User currentUser, Integer otherUserId) {

        // Tìm user kia
        User otherUser = userRepository.findById(otherUserId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        // Query tất cả đơn hàng giữa 2 người (cả 2 chiều buyer↔shopOwner)
        List<Order> orders = orderRepository.findOrdersBetweenUsers(currentUser, otherUser);

        return orders.stream().map(order -> {

            // ── Loại 1: OrderDetail → Product (sản phẩm thường)
            List<ChatOrderResponse.OrderItemPreview> detailPreviews = new ArrayList<>();
            if (order.getOrderDetails() != null) {
                for (OrderDetail od : order.getOrderDetails()) {
                    Product p = od.getProduct();
                    detailPreviews.add(ChatOrderResponse.OrderItemPreview.builder()
                            .name(p.getProductName())
                            .imageUrl(p.getImageUrl())
                            .quantity(od.getQuantity())
                            .unitPrice(od.getUnitPrice())
                            .type("PRODUCT")
                            .build());
                }
            }

            // ── Loại 2: BuildCombo (lấy ảnh từ product đầu tiên trong combo)
            List<ChatOrderResponse.OrderItemPreview> comboPreviews = new ArrayList<>();
            if (order.getBuildCombos() != null) {
                for (BuildCombo combo : order.getBuildCombos()) {
                    String comboImage = null;
                    if (combo.getItems() != null && !combo.getItems().isEmpty()) {
                        comboImage = combo.getItems().get(0).getProduct().getImageUrl();
                    }
                    comboPreviews.add(ChatOrderResponse.OrderItemPreview.builder()
                            .name(combo.getComboName())
                            .imageUrl(comboImage)
                            .quantity(1)
                            .unitPrice(combo.getDiscountPrice())
                            .type("COMBO")
                            .build());
                }
            }

            // ── Loại 3: MysteryBox (túi mù)
            List<ChatOrderResponse.OrderItemPreview> mysteryPreviews = new ArrayList<>();
            if (order.getMysteryBoxes() != null) {
                for (OrderMysteryBox orderBox : order.getMysteryBoxes()) {

                    MysteryBox box = orderBox.getMysteryBox();

                    mysteryPreviews.add(
                            ChatOrderResponse.OrderItemPreview.builder()
                                    .name(box.getBoxType())
                                    .quantity(orderBox.getQuantity())
                                    .unitPrice(box.getPrice())
                                    .type("MYSTERY_BOX")
                                    .build()
                    );
                }
            }

            // ── Preview đại diện: ưu tiên product → combo → mystery
            String previewName  = null;
            String previewImage = null;
            if (!detailPreviews.isEmpty()) {
                previewName  = detailPreviews.get(0).getName();
                previewImage = detailPreviews.get(0).getImageUrl();
            } else if (!comboPreviews.isEmpty()) {
                previewName  = comboPreviews.get(0).getName();
                previewImage = comboPreviews.get(0).getImageUrl();
            } else if (!mysteryPreviews.isEmpty()) {
                previewName  = mysteryPreviews.get(0).getName();
                previewImage = mysteryPreviews.get(0).getImageUrl();
            }

            int totalItems = detailPreviews.size() + comboPreviews.size() + mysteryPreviews.size();

            return ChatOrderResponse.builder()
                    .orderId(order.getId())
                    .status(order.getStatus())
                    .totalAmount(order.getTotalAmount())
                    .createdAt(order.getCreatedAt())
                    .previewItemName(previewName)
                    .previewImageUrl(previewImage)
                    .totalItems(totalItems)
                    .orderDetails(detailPreviews)
                    .combos(comboPreviews)
                    .mysteryBoxes(mysteryPreviews)
                    .build();

        }).collect(Collectors.toList());
    }

    // ==================== HELPER ====================

    /**
     * Tạo roomKey chuẩn: min(id1,id2) + "_" + max(id1,id2)
     * Đảm bảo cùng 1 cặp user luôn cho cùng 1 roomKey dù gọi theo thứ tự nào
     */
    private String buildRoomKey(Integer id1, Integer id2) {
        return Math.min(id1, id2) + "_" + Math.max(id1, id2);
    }

    private ChatMessageResponse toResponse(ChatMessage msg) {
        return ChatMessageResponse.builder()
                .id(msg.getId())
                .conversationId(msg.getConversation().getId())
                .senderId(msg.getSender().getId())
                .senderName(msg.getSender().getFullName())
                .content(msg.getContent())
                .sentAt(msg.getSentAt())
                .isRead(msg.isRead())
                .build();
    }
}