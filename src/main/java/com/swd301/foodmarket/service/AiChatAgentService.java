package com.swd301.foodmarket.service;

import com.swd301.foodmarket.dto.request.AiChatRequest;
import com.swd301.foodmarket.dto.response.AiChatResponse;
import com.swd301.foodmarket.entity.User;

public interface AiChatAgentService {

    AiChatResponse handleMessage(User user, AiChatRequest request);
}
