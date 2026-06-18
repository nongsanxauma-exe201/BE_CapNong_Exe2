package com.swd301.foodmarket.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swd301.foodmarket.ai.util.AiQuantityConverter;
import com.swd301.foodmarket.dto.internal.AiLlmIntentPayload;
import com.swd301.foodmarket.dto.internal.AiLlmItemPayload;
import com.swd301.foodmarket.dto.request.AiDetectedIntent;
import com.swd301.foodmarket.dto.request.AiItemCommand;
import com.swd301.foodmarket.enums.AiAccountActionType;
import com.swd301.foodmarket.enums.AiIntentType;
import com.swd301.foodmarket.enums.AiItemType;
import com.swd301.foodmarket.service.AiIntentClassifierService;
import com.swd301.foodmarket.service.GeminiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiIntentClassifierServiceImpl implements AiIntentClassifierService {

    private static final Pattern JSON_BLOCK_PATTERN =
            Pattern.compile("```(?:json)?\\s*([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);

    private static final Pattern JSON_OBJECT_PATTERN =
            Pattern.compile("\\{[\\s\\S]*}");

    private final GeminiService geminiService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public AiDetectedIntent classify(String message) {
        if (message == null || message.isBlank()) {
            return null;
        }

        String prompt = buildPrompt(message.trim());
        String rawResponse = geminiService.generateResponse(prompt, null);
        log.debug("LLM intent raw response: {}", rawResponse);

        AiLlmIntentPayload payload = parsePayload(rawResponse);
        if (payload == null || payload.getIntent() == null || payload.getIntent().isBlank()) {
            log.warn("LLM intent payload empty for message: {}", message);
            return null;
        }

        return mapToDetectedIntent(message, payload);
    }

    private String buildPrompt(String message) {
        return """
                Bạn là bộ phân tích ý định (intent router) cho chatbot cửa hàng nông sản Food Market.
                Nhiệm vụ: đọc CÂU KHÁCH, hiểu Ý NGHĨA thực sự, trả về JSON thuần (không markdown, không giải thích).
                KHÔNG tìm keyword bằng cách cắt chữ — hãy hiểu ngữ cảnh tiếng Việt tự nhiên.

                === CÁC INTENT ===

                1) LIST_CATALOG — khách hỏi shop đang bán gì, có hàng không, liệt kê sản phẩm
                   Ví dụ: "có sản phẩm không", "shop bán gì", "có những món nào đang bán"
                   JSON: { "intent": "LIST_CATALOG", "productName": null, "items": [] }

                2) SEARCH_ITEM — khách muốn tìm/xem/giá/hỏi về MỘT món cụ thể
                   Ví dụ: "tìm xoài", "có xoài không", "giá gừng bao nhiêu", "xem chi tiết về gừng", "cho tôi biết về rau muống"
                   productName = CHỈ tên món/sản phẩm (vd: "gừng", "xoài cát", "rau muống"), không gồm từ "chi tiết", "xem", "tìm", "về"
                   JSON: { "intent": "SEARCH_ITEM", "productName": "gừng", "items": [] }

                3) BUY_ITEM — khách muốn mua/thêm vào giỏ
                   Ví dụ: "mua 1kg gừng", "cho tôi 2 bó rau muống", "thêm 500g thịt bò vào giỏ"
                   JSON: { "intent": "BUY_ITEM", "productName": null, "items": [{ "name": "gừng", "quantity": 1, "unit": "kg" }] }

                4) CHECKOUT — thanh toán, chốt đơn
                   Ví dụ: "thanh toán", "checkout", "chốt đơn giúp em"
                   JSON: { "intent": "CHECKOUT", "productName": null, "items": [] }

                5) SELECT_OPTION — chọn số trong danh sách vừa hiện
                   Ví dụ: "chọn 2", "số 1", "lấy loại 3"
                   JSON: { "intent": "SELECT_OPTION", "selectedIndex": 2, "items": [] }

                6) ASK_INFORMATION — hỏi chính sách/chung, KHÔNG liên quan món cụ thể
                   Ví dụ: "phí ship tính sao", "giao hàng bao lâu", "shop ở đâu", "đổi trả thế nào"
                   JSON: { "intent": "ASK_INFORMATION", "topic": "phí ship", "items": [] }

                7) ACCOUNT_ACTION — thao tác tài khoản/đơn hàng trên web (CẦN đăng nhập)
                   accountAction hợp lệ:
                   - VIEW_PROFILE: xem thông tin tài khoản, hồ sơ cá nhân
                   - EDIT_PROFILE: cập nhật/sửa thông tin cá nhân (chỉ dẫn sang trang hồ sơ)
                   - VIEW_ORDERS: xem danh sách đơn hàng của tôi
                   - VIEW_ORDER_DETAIL: xem chi tiết 1 đơn (kèm orderId nếu có)
                   - VIEW_CART: xem giỏ hàng, liệt kê món trong giỏ
                   - VIEW_WALLET: xem ví, số dư
                   - VIEW_RETURNS: xem đơn trả hàng
                   Ví dụ: "xem tài khoản", "đơn hàng của tôi", "liệt kê món trong giỏ", "giỏ có gì", "số dư ví", "xem đơn 45"
                   JSON: { "intent": "ACCOUNT_ACTION", "accountAction": "VIEW_ORDERS", "orderId": null, "items": [] }

                8) UNKNOWN — không hiểu
                   JSON: { "intent": "UNKNOWN", "items": [] }

                === BẢO MẬT (bắt buộc) ===
                - KHÔNG trả userId, email người khác, mật khẩu, OTP
                - KHÔNG hỗ trợ: đổi mật khẩu, rút tiền, xóa tài khoản qua chat → trả ASK_INFORMATION hoặc hướng dẫn vào trang web
                - orderId chỉ dùng khi khách nói rõ số đơn

                === QUY TẮC ===
                - Phân biệt LIST_CATALOG (hỏi chung shop bán gì) vs SEARCH_ITEM (hỏi 1 món cụ thể)
                - "có xoài không" => SEARCH_ITEM, productName: "xoài"
                - "có sản phẩm không" => LIST_CATALOG
                - Đơn vị: kg, g, gram, bó, nải, hộp, túi, quả, chai...
                - quantity mặc định 1 nếu khách không nói rõ
                - Có thể nhiều món trong items khi BUY_ITEM

                === SCHEMA JSON (bắt buộc) ===
                {
                  "intent": "BUY_ITEM|SEARCH_ITEM|LIST_CATALOG|CHECKOUT|SELECT_OPTION|ASK_INFORMATION|ACCOUNT_ACTION|UNKNOWN",
                  "productName": "string hoặc null",
                  "selectedIndex": number hoặc null,
                  "topic": "string hoặc null",
                  "accountAction": "VIEW_PROFILE|EDIT_PROFILE|VIEW_ORDERS|VIEW_ORDER_DETAIL|VIEW_CART|VIEW_WALLET|VIEW_RETURNS hoặc null",
                  "orderId": number hoặc null,
                  "items": [
                    { "name": "tên món", "quantity": 1, "unit": "kg" }
                  ]
                }

                CÂU KHÁCH: %s
                """.formatted(message);
    }

    private AiLlmIntentPayload parsePayload(String rawResponse) {
        if (rawResponse == null || rawResponse.isBlank()) {
            return null;
        }

        String json = extractJson(rawResponse);
        if (json == null || json.isBlank()) {
            return null;
        }

        try {
            return objectMapper.readValue(json, AiLlmIntentPayload.class);
        } catch (Exception e) {
            log.warn("Failed to parse LLM intent JSON: {}", e.getMessage());
            return null;
        }
    }

    private String extractJson(String raw) {
        Matcher blockMatcher = JSON_BLOCK_PATTERN.matcher(raw);
        if (blockMatcher.find()) {
            return blockMatcher.group(1).trim();
        }

        Matcher objectMatcher = JSON_OBJECT_PATTERN.matcher(raw);
        if (objectMatcher.find()) {
            return objectMatcher.group().trim();
        }

        return raw.trim();
    }

    private AiDetectedIntent mapToDetectedIntent(String message, AiLlmIntentPayload payload) {
        AiIntentType intent = mapIntent(payload.getIntent());

        if (intent == AiIntentType.UNKNOWN) {
            return AiDetectedIntent.builder()
                    .intent(AiIntentType.UNKNOWN)
                    .rawMessage(message)
                    .build();
        }

        if (isListCatalogIntent(payload.getIntent())) {
            return AiDetectedIntent.builder()
                    .intent(AiIntentType.SEARCH_ITEM)
                    .rawMessage(message)
                    .keyword("")
                    .build();
        }

        AiDetectedIntent.AiDetectedIntentBuilder builder = AiDetectedIntent.builder()
                .intent(intent)
                .rawMessage(message);

        if (intent == AiIntentType.SEARCH_ITEM) {
            builder.keyword(resolveProductName(payload));
        }

        if (intent == AiIntentType.SELECT_ITEM_OPTION && payload.getSelectedIndex() != null) {
            builder.selectedIndex(payload.getSelectedIndex());
        }

        if (intent == AiIntentType.BUY_ITEM) {
            List<AiItemCommand> items = mapItems(payload.getItems());
            builder.items(items);
        }

        if (intent == AiIntentType.ACCOUNT_ACTION) {
            builder.accountAction(mapAccountAction(payload.getAccountAction()));
            builder.orderId(payload.getOrderId());
        }

        return builder.build();
    }

    private AiAccountActionType mapAccountAction(String rawAction) {
        if (rawAction == null || rawAction.isBlank()) {
            return null;
        }

        String normalized = rawAction.trim().toUpperCase();

        return switch (normalized) {
            case "VIEW_PROFILE", "PROFILE", "MY_PROFILE", "ACCOUNT" -> AiAccountActionType.VIEW_PROFILE;
            case "EDIT_PROFILE", "UPDATE_PROFILE", "EDIT_ACCOUNT" -> AiAccountActionType.EDIT_PROFILE;
            case "VIEW_ORDERS", "MY_ORDERS", "ORDERS", "ORDER_LIST" -> AiAccountActionType.VIEW_ORDERS;
            case "VIEW_ORDER_DETAIL", "ORDER_DETAIL", "TRACK_ORDER" -> AiAccountActionType.VIEW_ORDER_DETAIL;
            case "VIEW_CART", "MY_CART", "CART" -> AiAccountActionType.VIEW_CART;
            case "VIEW_WALLET", "MY_WALLET", "WALLET", "BALANCE" -> AiAccountActionType.VIEW_WALLET;
            case "VIEW_RETURNS", "MY_RETURNS", "RETURNS" -> AiAccountActionType.VIEW_RETURNS;
            default -> null;
        };
    }

    private String resolveProductName(AiLlmIntentPayload payload) {
        if (payload.getProductName() != null && !payload.getProductName().isBlank()) {
            return payload.getProductName().trim();
        }
        if (payload.getKeyword() != null && !payload.getKeyword().isBlank()) {
            return payload.getKeyword().trim();
        }
        return null;
    }

    private AiIntentType mapIntent(String rawIntent) {
        if (rawIntent == null || rawIntent.isBlank()) {
            return AiIntentType.UNKNOWN;
        }

        String normalized = rawIntent.trim().toUpperCase();

        return switch (normalized) {
            case "BUY_ITEM", "ADD_CART", "ADD_TO_CART", "ADDCARD", "ADD" -> AiIntentType.BUY_ITEM;
            case "SEARCH_ITEM", "SEARCH", "VIEW_DETAIL", "VIEW_PRODUCT" -> AiIntentType.SEARCH_ITEM;
            case "LIST_CATALOG", "LIST_PRODUCTS", "CATALOG" -> AiIntentType.SEARCH_ITEM;
            case "CHECKOUT" -> AiIntentType.CHECKOUT;
            case "SELECT_OPTION", "SELECT_ITEM_OPTION", "SELECT" -> AiIntentType.SELECT_ITEM_OPTION;
            case "ASK_INFORMATION", "ASK_INFO", "INFORMATION" -> AiIntentType.ASK_INFORMATION;
            case "ACCOUNT_ACTION", "ACCOUNT", "MY_ACCOUNT" -> AiIntentType.ACCOUNT_ACTION;
            case "UNKNOWN" -> AiIntentType.UNKNOWN;
            default -> AiIntentType.UNKNOWN;
        };
    }

    private boolean isListCatalogIntent(String rawIntent) {
        if (rawIntent == null) {
            return false;
        }

        String normalized = rawIntent.trim().toUpperCase();
        return normalized.equals("LIST_CATALOG")
                || normalized.equals("LIST_PRODUCTS")
                || normalized.equals("CATALOG");
    }

    private List<AiItemCommand> mapItems(List<AiLlmItemPayload> items) {
        List<AiItemCommand> result = new ArrayList<>();
        if (items == null) {
            return result;
        }

        for (AiLlmItemPayload item : items) {
            if (item == null || item.getName() == null || item.getName().isBlank()) {
                continue;
            }

            double quantity = item.getQuantity() != null && item.getQuantity() > 0
                    ? item.getQuantity()
                    : 1.0;

            String unit = item.getUnit() != null && !item.getUnit().isBlank()
                    ? AiQuantityConverter.normalizeUnit(item.getUnit())
                    : null;

            String name = item.getName().trim();

            result.add(AiItemCommand.builder()
                    .name(name)
                    .quantity(quantity)
                    .unit(unit)
                    .itemType(inferItemType(name))
                    .build());
        }

        return result;
    }

    private AiItemType inferItemType(String name) {
        String normalized = name.toLowerCase();

        if (normalized.contains("mystery")
                || normalized.contains("túi mù")
                || normalized.contains("tui mu")
                || normalized.contains("hộp bí ẩn")
                || normalized.contains("hop bi an")) {
            return AiItemType.MYSTERY_BOX;
        }

        return AiItemType.ANY;
    }
}
