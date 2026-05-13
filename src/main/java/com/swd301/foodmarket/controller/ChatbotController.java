package com.swd301.foodmarket.controller;

import com.swd301.foodmarket.dto.request.ChatRequest;
import com.swd301.foodmarket.dto.response.ApiResponse;
import com.swd301.foodmarket.dto.response.ChatHistoryResponse;
import com.swd301.foodmarket.dto.response.ChatResponse;
import com.swd301.foodmarket.service.ChatbotService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/chatbot")
@RequiredArgsConstructor
@Slf4j
public class ChatbotController {

    private final ChatbotService chatbotService;

    /**
     * Gửi tin nhắn và nhận phản hồi từ AI
     * POST /chatbot/chat
     */
    @PostMapping("/chat")
    public ApiResponse<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        log.info("Chat request received");
        return ApiResponse.<ChatResponse>builder()
                .result(chatbotService.chat(request))
                .message("Chat response generated successfully")
                .build();
    }

    /**
     * Lấy toàn bộ lịch sử chat của user hiện tại
     * GET /chatbot/history
     */
    @GetMapping("/history")
    public ApiResponse<List<ChatHistoryResponse>> getHistory() {
        log.info("Get chat history request");
        return ApiResponse.<List<ChatHistoryResponse>>builder()
                .result(chatbotService.getHistory())
                .message("Chat history retrieved successfully")
                .build();
    }

    /**
     * Xóa toàn bộ lịch sử chat của user hiện tại
     * DELETE /chatbot/history
     */
    @DeleteMapping("/history")
    public ApiResponse<Void> clearHistory() {
        log.info("Clear chat history request");
        chatbotService.clearHistory();
        return ApiResponse.<Void>builder()
                .message("Chat history cleared successfully")
                .build();
    }
}