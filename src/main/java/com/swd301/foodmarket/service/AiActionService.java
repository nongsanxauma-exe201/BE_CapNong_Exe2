package com.swd301.foodmarket.service;

import com.swd301.foodmarket.dto.response.AiChatResponse;
import com.swd301.foodmarket.dto.request.AiItemCommand;
import com.swd301.foodmarket.entity.User;

import java.util.List;

public interface AiActionService {

    AiChatResponse searchItems(User user, String sessionId, String keyword);

    AiChatResponse buyItems(User user, String sessionId, List<AiItemCommand> items);

    AiChatResponse selectItemOption(User user, String sessionId, Integer selectedIndex);

    AiChatResponse createCheckoutDraft(User user);
}
