package com.swd301.foodmarket.service;

import com.swd301.foodmarket.dto.response.AiChatResponse;
import com.swd301.foodmarket.entity.User;
import com.swd301.foodmarket.enums.AiAccountActionType;

public interface AiAccountActionService {

    AiChatResponse execute(User user, AiAccountActionType action, Integer orderId);
}
