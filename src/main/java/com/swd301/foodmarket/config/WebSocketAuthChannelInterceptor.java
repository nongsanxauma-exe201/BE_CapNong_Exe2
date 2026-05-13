package com.swd301.foodmarket.config;

import com.swd301.foodmarket.entity.User;
import com.swd301.foodmarket.exception.AppException;
import com.swd301.foodmarket.exception.ErrorCode;
import com.swd301.foodmarket.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtDecoder customJwtDecoder;
    private final UserRepository userRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null) return message;

        StompCommand command = accessor.getCommand();

        // ✅ Xử lý cả CONNECT và SEND (message từ shipper)
        if (StompCommand.CONNECT.equals(command) || StompCommand.SEND.equals(command)) {

            String authHeader = accessor.getFirstNativeHeader("Authorization");

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                try {
                    Jwt jwt = customJwtDecoder.decode(token);
                    String email = jwt.getSubject();

                    User user = userRepository.findByEmail(email)
                            .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

                    // ✅ Tạo Authentication object đầy đủ
                    String role = "ROLE_" + user.getRole().getName().name();
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    email,
                                    null,
                                    List.of(new SimpleGrantedAuthority(role))
                            );

                    // ✅ Set vào SecurityContext để service có thể dùng
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    // ✅ Set Principal vào accessor (cho private messaging)
                    String userId = user.getId().toString();
                    accessor.setUser(authentication);

                    if (StompCommand.CONNECT.equals(command)) {
                        log.info("WebSocket CONNECT | email={} | userId={}", email, userId);
                    }

                } catch (Exception e) {
                    log.warn("WebSocket auth failed: {}", e.getMessage());
                }
            } else if (StompCommand.CONNECT.equals(command)) {
                log.warn("WebSocket CONNECT - Missing Authorization header");
            }
        }

        return message;
    }
}