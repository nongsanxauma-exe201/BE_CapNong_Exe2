package com.swd301.foodmarket.service.impl;

import com.swd301.foodmarket.dto.request.AiChatRequest;
import com.swd301.foodmarket.dto.request.AiDetectedIntent;
import com.swd301.foodmarket.dto.response.AiChatResponse;
import com.swd301.foodmarket.entity.Product;
import com.swd301.foodmarket.enums.AiIntentType;
import com.swd301.foodmarket.enums.ProductStatus;
import com.swd301.foodmarket.entity.User;
import com.swd301.foodmarket.repository.ProductRepository;
import com.swd301.foodmarket.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AiChatAgentServiceImpl implements AiChatAgentService {

    private final AiIntentService aiIntentService;
    private final AiActionService aiActionService;
    private final RagService ragService;
    private final AiAnswerService aiAnswerService;
    private final ProductRepository productRepository;

    @Override
    public AiChatResponse handleMessage(User user, AiChatRequest request) {
        if (request == null || request.getMessage() == null || request.getMessage().isBlank()) {
            return AiChatResponse.text("Bạn muốn mình hỗ trợ gì?");
        }

        String message = request.getMessage();
        AiDetectedIntent detectedIntent = aiIntentService.detectIntent(message);

        AiIntentType intent = detectedIntent.getIntent();
        if (intent == null) {
            intent = AiIntentType.UNKNOWN;
        }

        return switch (intent) {
            case SEARCH_ITEM ->
                    aiActionService.searchItems(user, request.getSessionId(), detectedIntent.getKeyword());

            case BUY_ITEM ->
                    aiActionService.buyItems(user, request.getSessionId(), detectedIntent.getItems());

            case SELECT_ITEM_OPTION ->
                    aiActionService.selectItemOption(user, request.getSessionId(), detectedIntent.getSelectedIndex());

            case CHECKOUT ->
                    aiActionService.createCheckoutDraft(user);

            case ASK_INFORMATION -> {
                String context = ragService.buildContext(message);
                String catalogContext = buildProductCatalogContext();
                String mergedContext = mergeContext(catalogContext, context);
                String answer = aiAnswerService.answerWithContext(message, mergedContext);
                yield AiChatResponse.text(answer);
            }

            default ->
                    AiChatResponse.text("Mình chưa hiểu rõ ý bạn. Bạn có thể nói lại cụ thể hơn không?");
        };
    }

    private String mergeContext(String catalogContext, String ragContext) {
        StringBuilder sb = new StringBuilder();

        if (catalogContext != null && !catalogContext.isBlank()) {
            sb.append(catalogContext.trim());
        }

        if (ragContext != null && !ragContext.isBlank()) {
            if (!sb.isEmpty()) {
                sb.append("\n\n");
            }
            sb.append(ragContext.trim());
        }

        return sb.toString();
    }

    private String buildProductCatalogContext() {
        List<Product> allProducts = productRepository.findByStatusOrderByCategoryAsc(ProductStatus.AVAILABLE);

        if (allProducts.isEmpty()) {
            return "";
        }

        Map<String, List<String>> byCategory = new LinkedHashMap<>();
        for (Product product : allProducts) {
            String categoryName = product.getCategory() != null
                    ? product.getCategory().getName()
                    : "Khác";
            byCategory.computeIfAbsent(categoryName, key -> new ArrayList<>())
                    .add(product.getProductName());
        }

        StringBuilder sb = new StringBuilder();
        sb.append("DANH SÁCH SẢN PHẨM HIỆN CÓ (phân theo danh mục):\n");

        byCategory.forEach((category, names) -> {
            sb.append("[Danh mục: ").append(category).append("]\n");
            names.forEach(name -> sb.append("  - ").append(name).append("\n"));
            sb.append("\n");
        });

        return sb.toString();
    }
}
