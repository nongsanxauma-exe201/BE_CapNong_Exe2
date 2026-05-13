package com.swd301.foodmarket.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * CẬP NHẬT file WebSocketConfig.java hiện có trong project.
 * Chỉ thêm 2 thứ so với file cũ:
 *   1. Inject + đăng ký WebSocketAuthChannelInterceptor
 *   2. Thêm config.setUserDestinationPrefix("/user") cho private message
 *
 * Phần GPS giữ nguyên hoàn toàn.
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    // ← THÊM MỚI: inject interceptor
    private final WebSocketAuthChannelInterceptor webSocketAuthChannelInterceptor;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Giữ nguyên từ file cũ
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");

        // ← THÊM MỚI: prefix cho private message tới từng user
        // Server gửi: messagingTemplate.convertAndSendToUser(userId, "/queue/messages", data)
        // Client subscribe: /user/{myId}/queue/messages
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Giữ nguyên từ file cũ
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    /**
     * ← THÊM MỚI: đăng ký interceptor để validate JWT khi client CONNECT WebSocket
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(webSocketAuthChannelInterceptor);
    }
}