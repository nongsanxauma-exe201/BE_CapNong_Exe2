package com.swd301.foodmarket.service;

import com.swd301.foodmarket.dto.request.BotKnowledgeRequest;
import com.swd301.foodmarket.dto.response.BotKnowledgeResponse;
import com.swd301.foodmarket.enums.KnowledgeType;

import java.util.List;

public interface BotKnowledgeService {
    BotKnowledgeResponse create(BotKnowledgeRequest request);
    BotKnowledgeResponse update(Integer id, BotKnowledgeRequest request);
    void delete(Integer id);
    BotKnowledgeResponse getById(Integer id);
    List<BotKnowledgeResponse> getAll();
    List<BotKnowledgeResponse> getByType(KnowledgeType type);
}