package com.swd301.foodmarket.service.impl;

import com.swd301.foodmarket.dto.request.BotKnowledgeRequest;
import com.swd301.foodmarket.dto.response.BotKnowledgeResponse;
import com.swd301.foodmarket.entity.BotKnowledge;
import com.swd301.foodmarket.enums.KnowledgeType;
import com.swd301.foodmarket.exception.AppException;
import com.swd301.foodmarket.exception.ErrorCode;
import com.swd301.foodmarket.repository.BotKnowledgeRepository;
import com.swd301.foodmarket.service.BotKnowledgeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BotKnowledgeServiceImpl implements BotKnowledgeService {

    private final BotKnowledgeRepository botKnowledgeRepository;

    @Override
    public BotKnowledgeResponse create(BotKnowledgeRequest request) {
        BotKnowledge knowledge = BotKnowledge.builder()
                .type(request.getType())
                .title(request.getTitle())
                .content(request.getContent())
                .keywords(request.getKeywords())
                .active(request.getActive() != null ? request.getActive() : true)
                .build();

        BotKnowledge saved = botKnowledgeRepository.save(knowledge);
        log.info("BotKnowledge created with ID: {}", saved.getId());
        return toResponse(saved);
    }

    @Override
    public BotKnowledgeResponse update(Integer id, BotKnowledgeRequest request) {
        BotKnowledge knowledge = botKnowledgeRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.BOT_KNOWLEDGE_NOT_FOUND));

        knowledge.setType(request.getType());
        knowledge.setTitle(request.getTitle());
        knowledge.setContent(request.getContent());
        knowledge.setKeywords(request.getKeywords());
        if (request.getActive() != null) {
            knowledge.setActive(request.getActive());
        }

        BotKnowledge updated = botKnowledgeRepository.save(knowledge);
        log.info("BotKnowledge updated with ID: {}", updated.getId());
        return toResponse(updated);
    }

    @Override
    public void delete(Integer id) {
        if (!botKnowledgeRepository.existsById(id)) {
            throw new AppException(ErrorCode.BOT_KNOWLEDGE_NOT_FOUND);
        }
        botKnowledgeRepository.deleteById(id);
        log.info("BotKnowledge deleted with ID: {}", id);
    }

    @Override
    public BotKnowledgeResponse getById(Integer id) {
        BotKnowledge knowledge = botKnowledgeRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.BOT_KNOWLEDGE_NOT_FOUND));
        return toResponse(knowledge);
    }

    @Override
    public List<BotKnowledgeResponse> getAll() {
        return botKnowledgeRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<BotKnowledgeResponse> getByType(KnowledgeType type) {
        return botKnowledgeRepository.findByTypeAndActiveTrue(type)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ===== HELPER =====
    private BotKnowledgeResponse toResponse(BotKnowledge k) {
        return BotKnowledgeResponse.builder()
                .id(k.getId())
                .type(k.getType())
                .title(k.getTitle())
                .content(k.getContent())
                .keywords(k.getKeywords())
                .active(k.getActive())
                .createdAt(k.getCreatedAt())
                .updatedAt(k.getUpdatedAt())
                .build();
    }
}