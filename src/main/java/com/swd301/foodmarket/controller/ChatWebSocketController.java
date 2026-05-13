package com.swd301.foodmarket.controller;

import com.swd301.foodmarket.dto.request.ChatMessageRequest;
import com.swd301.foodmarket.dto.response.ChatMessageResponse;
import com.swd301.foodmarket.entity.User;
import com.swd301.foodmarket.exception.AppException;
import com.swd301.foodmarket.exception.ErrorCode;
import com.swd301.foodmarket.repository.UserRepository;
import com.swd301.foodmarket.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatWebSocketController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;

    /**
     * Client gửi tới: /app/chat.send
     * Body JSON: { "receiverId": 5, "content": "Hello!" }
     *
     * Server gửi lại tới topic riêng của từng conversation:
     * /topic/chat.{roomKey}  VD: /topic/chat.2_3
     * => Cả 2 người trong conversation đều subscribe topic này => đều nhận được
     */
    @MessageMapping("/chat.send")
    public void sendMessage(@Payload ChatMessageRequest request, Principal principal) {
        User sender = extractUser(principal);
        if (sender == null) {
            log.warn("Rejected unauthenticated WebSocket message");
            return;
        }

        log.info("WS message: {} → {}: {}", sender.getId(), request.getReceiverId(), request.getContent());

        ChatMessageResponse response = chatService.sendMessage(sender, request);

        // Tạo roomKey giống logic trong service: min_max
        int id1 = sender.getId();
        int id2 = request.getReceiverId();
        String roomKey = Math.min(id1, id2) + "_" + Math.max(id1, id2);

        // Gửi tới topic của conversation này
        // Client subscribe: /topic/chat.2_3
        messagingTemplate.convertAndSend("/topic/chat." + roomKey, response);

        log.info("Broadcast to /topic/chat.{}", roomKey);
    }

    @MessageMapping("/chat.read")
    public void markAsRead(@Payload ChatMessageRequest request, Principal principal) {
        User currentUser = extractUser(principal);
        if (currentUser == null) return;
        chatService.markAsRead(currentUser, request.getReceiverId());
    }

    private User extractUser(Principal principal) {
        if (principal == null) return null;
        String email = principal.getName();
        return userRepository.findByEmail(email).orElse(null);
    }
}