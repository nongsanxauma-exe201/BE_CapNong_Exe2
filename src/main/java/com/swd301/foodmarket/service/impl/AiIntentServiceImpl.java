package com.swd301.foodmarket.service.impl;

import com.swd301.foodmarket.ai.util.AiQuantityConverter;
import com.swd301.foodmarket.dto.request.AiDetectedIntent;
import com.swd301.foodmarket.dto.request.AiItemCommand;
import com.swd301.foodmarket.enums.AiIntentType;
import com.swd301.foodmarket.enums.AiItemType;
import com.swd301.foodmarket.service.AiIntentClassifierService;
import com.swd301.foodmarket.service.AiIntentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiIntentServiceImpl implements AiIntentService {

    private final AiIntentClassifierService aiIntentClassifierService;

    private static final Pattern SELECT_OPTION_PATTERN =
            Pattern.compile("^(?:chon|chọn|lay|lấy|mua|them|thêm)?\\s*(?:loai|loại|so|số)?\\s*(\\d+)$");

    private static final Pattern PURE_NUMBER_PATTERN = Pattern.compile("^(\\d+)$");

    @Override
    public AiDetectedIntent detectIntent(String message) {
        if (message == null || message.isBlank()) {
            return AiDetectedIntent.builder()
                    .intent(AiIntentType.UNKNOWN)
                    .rawMessage(message)
                    .build();
        }

        AiDetectedIntent fastPath = detectFastPath(message);
        if (fastPath != null) {
            return fastPath;
        }

        try {
            AiDetectedIntent llmIntent = aiIntentClassifierService.classify(message);
            if (isUsableLlmIntent(llmIntent)) {
                return normalizeLlmIntent(message, llmIntent);
            }
            log.warn("LLM intent not usable for message: {}", message);
        } catch (Exception e) {
            log.warn("LLM intent classification failed: {}", e.getMessage());
        }

        return AiDetectedIntent.builder()
                .intent(AiIntentType.ASK_INFORMATION)
                .rawMessage(message)
                .build();
    }

    /** Chỉ bỏ qua LLM khi user chọn số option ("1", "chọn 2"). */
    private AiDetectedIntent detectFastPath(String message) {
        String normalized = normalize(message);

        Matcher selectMatcher = SELECT_OPTION_PATTERN.matcher(normalized);
        if (selectMatcher.matches()) {
            return AiDetectedIntent.builder()
                    .intent(AiIntentType.SELECT_ITEM_OPTION)
                    .rawMessage(message)
                    .selectedIndex(Integer.parseInt(selectMatcher.group(1)))
                    .build();
        }

        Matcher pureNumberMatcher = PURE_NUMBER_PATTERN.matcher(normalized);
        if (pureNumberMatcher.matches()) {
            return AiDetectedIntent.builder()
                    .intent(AiIntentType.SELECT_ITEM_OPTION)
                    .rawMessage(message)
                    .selectedIndex(Integer.parseInt(pureNumberMatcher.group(1)))
                    .build();
        }

        return null;
    }

    private boolean isUsableLlmIntent(AiDetectedIntent intent) {
        if (intent == null || intent.getIntent() == null) {
            return false;
        }

        if (intent.getIntent() == AiIntentType.UNKNOWN) {
            return false;
        }

        if (intent.getIntent() == AiIntentType.BUY_ITEM) {
            return intent.getItems() != null && !intent.getItems().isEmpty();
        }

        if (intent.getIntent() == AiIntentType.SEARCH_ITEM) {
            return true;
        }

        if (intent.getIntent() == AiIntentType.SELECT_ITEM_OPTION) {
            return intent.getSelectedIndex() != null && intent.getSelectedIndex() > 0;
        }

        return true;
    }

    private AiDetectedIntent normalizeLlmIntent(String message, AiDetectedIntent llmIntent) {
        llmIntent.setRawMessage(message);

        if (llmIntent.getIntent() == AiIntentType.SEARCH_ITEM
                && (llmIntent.getKeyword() == null || llmIntent.getKeyword().isBlank())) {
            llmIntent.setKeyword("");
        }

        if (llmIntent.getIntent() == AiIntentType.BUY_ITEM && llmIntent.getItems() != null) {
            for (AiItemCommand item : llmIntent.getItems()) {
                if (item.getUnit() != null) {
                    item.setUnit(AiQuantityConverter.normalizeUnit(item.getUnit()));
                }
                if (item.getItemType() == null) {
                    item.setItemType(inferItemType(item.getName()));
                }
            }
        }

        return llmIntent;
    }

    private AiItemType inferItemType(String text) {
        if (text == null) {
            return AiItemType.ANY;
        }

        String normalized = normalize(text);
        if (normalized.contains("mystery")
                || normalized.contains("tui mu")
                || normalized.contains("tui bi an")
                || normalized.contains("hop bi an")) {
            return AiItemType.MYSTERY_BOX;
        }

        return AiItemType.ANY;
    }

    private String normalize(String text) {
        if (text == null) {
            return "";
        }

        String temp = Normalizer.normalize(text, Normalizer.Form.NFD);

        return Pattern.compile("\\p{InCombiningDiacriticalMarks}+")
                .matcher(temp)
                .replaceAll("")
                .toLowerCase()
                .replace("đ", "d")
                .replaceAll("[^a-z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
