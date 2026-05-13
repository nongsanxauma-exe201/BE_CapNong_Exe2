package com.swd301.foodmarket.service;

import com.swd301.foodmarket.dto.request.ChatRequest;
import com.swd301.foodmarket.dto.response.ChatHistoryResponse;
import com.swd301.foodmarket.dto.response.ChatResponse;

import java.util.List;

public interface ChatbotService {
    public ChatResponse chat(ChatRequest request);
    public List<ChatHistoryResponse> getHistory();
    public void clearHistory();
}
