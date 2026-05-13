package com.swd301.foodmarket.service.impl;

import com.swd301.foodmarket.entity.BotKnowledge;
import com.swd301.foodmarket.repository.BotKnowledgeRepository;
import com.swd301.foodmarket.service.RagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RagServiceImpl implements RagService {

    private final BotKnowledgeRepository botKnowledgeRepository;

    private static final int MAX_CHUNKS = 20;

    private static final List<String> STOP_WORDS = List.of(
            "la", "gi", "co", "khong", "the", "nao", "duoc", "thi", "va",
            "cua", "trong", "voi", "nhu", "cho", "khi", "ma", "hay", "se",
            "da", "roi", "biet", "hieu", "noi", "ve", "moi", "tat", "ca",
            "nhung", "cac", "mot", "hai", "ba", "do", "ay", "nay", "kia",
            "sao", "dau", "di", "len", "xuong", "vao", "ra", "tu", "ban",
            "minh", "toi", "ho", "no", "ai", "muon", "can", "het", "them"
    );

    /**
     * Build context từ DB dựa theo câu hỏi của user.
     * Trả về chuỗi context để nhét vào prompt Gemini.
     */
    @Override
    public String buildContext(String question) {
        List<String> keywords = extractKeywords(question);

        if (keywords.isEmpty()) {
            // Fallback: lấy tất cả knowledge active
            return buildContextFromChunks(
                    botKnowledgeRepository.findByActiveTrue()
            );
        }

        // Tìm kiếm theo từng keyword, gom kết quả, loại trùng
        Map<Integer, BotKnowledge> resultMap = new LinkedHashMap<>();

        for (String keyword : keywords) {
            List<BotKnowledge> found = botKnowledgeRepository.searchByKeyword(keyword);
            found.forEach(k -> resultMap.put(k.getId(), k));
        }

        List<BotKnowledge> matched = resultMap.values()
                .stream()
                .limit(MAX_CHUNKS)
                .collect(Collectors.toList());

        if (matched.isEmpty()) {
            log.info("RAG: No knowledge matched for question: {}", question);
            return "";
        }

        log.info("RAG: Found {} knowledge chunks for question: {}", matched.size(), question);
        return buildContextFromChunks(matched);
    }

    // ===== PRIVATE HELPERS =====

    private String buildContextFromChunks(List<BotKnowledge> chunks) {
        if (chunks.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        for (BotKnowledge chunk : chunks) {
            sb.append("[").append(chunk.getType()).append("] ")
                    .append(chunk.getTitle()).append("\n")
                    .append(chunk.getContent()).append("\n\n");
        }
        return sb.toString().trim();
    }

    private List<String> extractKeywords(String text) {
        String normalized = normalize(text);
        String[] words = normalized.split("\\s+");

        return Arrays.stream(words)
                .filter(w -> w.length() > 2)
                .filter(w -> !STOP_WORDS.contains(w))
                .distinct()
                .collect(Collectors.toList());
    }

    private String normalize(String text) {
        if (text == null) return "";
        String temp = Normalizer.normalize(text, Normalizer.Form.NFD);
        return Pattern.compile("\\p{InCombiningDiacriticalMarks}+")
                .matcher(temp)
                .replaceAll("")
                .toLowerCase()
                .replace("đ", "d")
                .replaceAll("[^a-z0-9\\s]", "")
                .trim();
    }

    public String normalizePublic(String text) {
        return normalize(text);
    }
}