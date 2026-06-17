package com.swd301.foodmarket.controller;

import com.swd301.foodmarket.dto.request.AiChatRequest;
import com.swd301.foodmarket.dto.response.AiChatResponse;
import com.swd301.foodmarket.dto.response.ApiResponse;
import com.swd301.foodmarket.entity.User;
import com.swd301.foodmarket.repository.UserRepository;
import com.swd301.foodmarket.service.AiChatAgentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
@Slf4j
public class AiChatController {

    private final AiChatAgentService aiChatAgentService;
    private final UserRepository userRepository;

    @PostMapping("/chat")
    public ApiResponse<AiChatResponse> chat(@RequestBody AiChatRequest request) {
        User currentUser = getCurrentUserOrNull();
        log.info("AI chat request from user: {}", currentUser != null ? currentUser.getId() : "anonymous");

        AiChatResponse response = aiChatAgentService.handleMessage(currentUser, request);

        return ApiResponse.<AiChatResponse>builder()
                .result(response)
                .message("AI chat response generated successfully")
                .build();
    }

    private User getCurrentUserOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }

        String principal = auth.getName();
        if (principal == null || "anonymousUser".equals(principal)) {
            return null;
        }

        return userRepository.findByEmail(principal).orElse(null);
    }
}
