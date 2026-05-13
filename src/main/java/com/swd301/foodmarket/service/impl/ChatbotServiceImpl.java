package com.swd301.foodmarket.service.impl;

import com.swd301.foodmarket.dto.request.ChatRequest;
import com.swd301.foodmarket.dto.response.ChatHistoryResponse;
import com.swd301.foodmarket.dto.response.ChatResponse;
import com.swd301.foodmarket.entity.ChatBot;
import com.swd301.foodmarket.entity.Product;
import com.swd301.foodmarket.entity.User;
import com.swd301.foodmarket.enums.ProductStatus;
import com.swd301.foodmarket.exception.AppException;
import com.swd301.foodmarket.exception.ErrorCode;
import com.swd301.foodmarket.repository.ChatBotRepository;
import com.swd301.foodmarket.repository.ProductRepository;
import com.swd301.foodmarket.repository.UserRepository;
import com.swd301.foodmarket.service.ChatbotService;
import com.swd301.foodmarket.service.GeminiService;
import com.swd301.foodmarket.service.RagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatbotServiceImpl implements ChatbotService {

    private final GeminiService geminiService;
    private final ChatBotRepository chatBotRepository;
    private final UserRepository userRepository;
    private final RagService ragService;
    private final ProductRepository productRepository;

    @Override
    public ChatResponse chat(ChatRequest request) {
        User currentUser = getCurrentUser();

        List<ChatBot> recentHistory = chatBotRepository
                .findTop10ByUser_IdOrderByCreatedAtDesc(currentUser.getId());
        Collections.reverse(recentHistory);
        String ragContext = ragService.buildContext(request.getMessage());

        String finalPrompt = buildPrompt(ragContext, recentHistory, request.getMessage());
        String botReply = geminiService.generateResponse(finalPrompt, request.getUserApiKey());

        // Tìm sản phẩm liên quan dựa theo câu hỏi của user
        List<ChatResponse.ProductSuggestion> suggestedProducts =
                findSuggestedProducts(request.getMessage(), botReply);

        saveMessage(currentUser, "USER", request.getMessage());
        saveMessage(currentUser, "BOT", botReply);

        return ChatResponse.builder()
                .reply(botReply)
                .timestamp(LocalDateTime.now())
                .suggestedProducts(suggestedProducts)
                .build();
    }
    @Override
    public List<ChatHistoryResponse> getHistory() {
        User currentUser = getCurrentUser();

        return chatBotRepository
                .findByUser_IdOrderByCreatedAtAsc(currentUser.getId())
                .stream()
                .map(msg -> ChatHistoryResponse.builder()
                        .id(msg.getId())
                        .role(msg.getRole())
                        .content(msg.getContent())
                        .createdAt(msg.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }
    @Override
    public void clearHistory() {
        User currentUser = getCurrentUser();
        List<ChatBot> messages = chatBotRepository
                .findByUser_IdOrderByCreatedAtAsc(currentUser.getId());
        chatBotRepository.deleteAll(messages);
        log.info("Chat history cleared for user ID: {}", currentUser.getId());
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }

    private void saveMessage(User user, String role, String content) {
        ChatBot msg = ChatBot.builder()
                .user(user)
                .role(role)
                .content(content)
                .build();
        chatBotRepository.save(msg);
    }

    private String buildPrompt(String ragContext,
                               List<ChatBot> history,
                               String userMessage) {
        StringBuilder sb = new StringBuilder();

        // System prompt
        sb.append("""
    Bạn là trợ lý ảo của Food Market - nền tảng thương mại điện tử chuyên về rau, củ, quả tươi sạch.
    Nhiệm vụ:
    1. Hỗ trợ khách hàng về đơn hàng, sản phẩm rau củ quả, chính sách giao hàng, đổi trả.
    2. Tư vấn thực đơn và lên kế hoạch ăn uống (build plan):
       - Nếu chưa nói số ngày, PHẢI hỏi muốn lên kế hoạch bao nhiêu ngày trước.
       - Nếu chưa nói rõ nhu cầu, PHẢI hỏi thêm: mục tiêu, số người ăn, ngân sách.
       - KHÔNG được tự đoán hoặc tự chọn mục tiêu khi buyer chưa xác nhận.
       - Chỉ sau khi có đủ thông tin mới lên thực đơn cụ thể.
       - Khi liệt kê nguyên liệu, PHẢI viết mỗi nguyên liệu trên 1 dòng riêng.
       - KHÔNG gộp nhiều nguyên liệu vào cùng 1 dòng hoặc trong dấu ngoặc.
    3. Trả lời lịch sự, ngắn gọn, rõ ràng bằng tiếng Việt.
    4. Ưu tiên dùng thông tin trong TÀI LIỆU bên dưới.
    5. Nếu tài liệu không có thông tin, dùng kiến thức chung nhưng phải nói rõ.
    6. KHÔNG bịa đặt thông tin về đơn hàng hay sản phẩm cụ thể.
    7. Chỉ tư vấn sản phẩm liên quan đến rau, củ, quả.
    8. KHI được hỏi về danh sách sản phẩm/loại rau củ quả, hãy liệt kê ĐẦY ĐỦ
       từ DANH SÁCH SẢN PHẨM bên dưới, viết mỗi sản phẩm trên 1 dòng riêng.
    9. PHÂN BIỆT ĐÚNG DANH MỤC KHI TRẢ LỜI:
       - Nếu hỏi "rau" hoặc "rau lá" → CHỈ liệt kê sản phẩm thuộc danh mục "Rau lá".
       - Nếu hỏi "củ" → CHỈ liệt kê sản phẩm thuộc danh mục "Củ".
       - Nếu hỏi "quả" hoặc "trái cây" → CHỈ liệt kê sản phẩm thuộc danh mục "Quả/Trái cây".
       - Nếu hỏi chung "rau củ quả" hoặc "tất cả" → liệt kê tất cả danh mục.
       - KHÔNG được trộn lẫn sản phẩm giữa các danh mục khi người dùng hỏi cụ thể 1 danh mục.
    """);

        // ✅ Inject product catalog động từ DB
        sb.append(buildProductCatalogContext());

        // RAG context
        if (ragContext != null && !ragContext.isBlank()) {
            sb.append("\nTÀI LIỆU THAM KHẢO:\n").append(ragContext).append("\n");
        }

        // Lịch sử hội thoại
        if (!history.isEmpty()) {
            sb.append("\nLỊCH SỬ HỘI THOẠI GẦN ĐÂY:\n");
            for (ChatBot h : history) {
                String role = "USER".equals(h.getRole()) ? "Khách" : "Trợ lý";
                sb.append(role).append(": ").append(h.getContent()).append("\n");
            }
        }

        // Câu hỏi hiện tại
        sb.append("\nKhách: ").append(userMessage).append("\nTrợ lý:");

        return sb.toString();
    }

    /**
     * Tìm sản phẩm liên quan dựa theo keyword trong câu hỏi + bot reply
     * Gộp cả 2 để tăng độ chính xác
     */
    private List<ChatResponse.ProductSuggestion> findSuggestedProducts(
            String userMessage, String botReply) {
        try {
            String combinedText = userMessage + " " + botReply;
            List<String> keywords = extractKeywordsForProduct(combinedText);

            if (keywords.isEmpty()) return List.of();

            // ✅ Bỏ limit - để tự nhiên bao nhiêu match ra bấy nhiêu
            Set<Product> matched = new LinkedHashSet<>();
            for (String keyword : keywords) {
                List<Product> found = productRepository
                        .findByProductNameContainingIgnoreCaseAndStatus(
                                keyword, ProductStatus.AVAILABLE
                        );
                matched.addAll(found);
            }

            return matched.stream()
                    .map(p -> ChatResponse.ProductSuggestion.builder()
                            .id(p.getId())
                            .name(p.getProductName())
                            .price(p.getSellingPrice())
                            .imageUrl(p.getImageUrl())
                            .category(p.getCategory() != null
                                    ? p.getCategory().getName() : null)
                            .build())
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.warn("Could not find suggested products: {}", e.getMessage());
            return List.of();
        }
    }

    // Extract keyword ngắn gọn cho việc tìm sản phẩm
// Khác với RAG keyword, cái này giữ lại tên thực phẩm cụ thể
    private List<String> extractKeywordsForProduct(String text) {
        List<Product> allProducts = productRepository
                .findByStatusOrderByCategoryAsc(ProductStatus.AVAILABLE);

        if (allProducts.isEmpty()) return List.of();

        // ✅ Chỉ lowercase, giữ nguyên dấu tiếng Việt
        // Thêm space để tách câu thành các từ rõ ràng
        String normalizedText = " " + text.toLowerCase()
                .replaceAll("[\\*\\#\\-\\.\\,\\!\\?\\:\\;\\(\\)\\[\\]\\\"\\']", " ")
                .replaceAll("\\s+", " ")
                .trim() + " ";

        List<String> matchedNames = new ArrayList<>();

        for (Product p : allProducts) {
            String productName = p.getProductName();
            String normalizedName = " " + productName.toLowerCase() + " ";

            // ✅ Whole-word match với dấu tiếng Việt nguyên vẹn
            if (normalizedText.contains(normalizedName)) {
                matchedNames.add(productName);
                continue;
            }

            // ✅ Match từng từ (>= 3 ký tự) của tên sản phẩm
            String[] nameParts = productName.toLowerCase().split("\\s+");
            if (nameParts.length >= 2) {
                boolean allFound = Arrays.stream(nameParts)
                        .filter(w -> w.length() >= 3)
                        .allMatch(w -> normalizedText.contains(" " + w + " ")
                                || normalizedText.contains(" " + w + "\n"));
                if (allFound) {
                    matchedNames.add(productName);
                }
            }
        }

        log.info("[Product keywords] input='{}...' → matched={}",
                text.substring(0, Math.min(40, text.length())), matchedNames);
        return matchedNames;
    }

    private String buildProductCatalogContext() {
        try {
            List<Product> allProducts = productRepository
                    .findByStatusOrderByCategoryAsc(ProductStatus.AVAILABLE);

            if (allProducts.isEmpty()) return "";

            // Group theo category
            Map<String, List<String>> byCategory = new LinkedHashMap<>();
            for (Product p : allProducts) {
                String catName = p.getCategory() != null
                        ? p.getCategory().getName() : "Khác";
                byCategory.computeIfAbsent(catName, k -> new ArrayList<>())
                        .add(p.getProductName());
            }

            StringBuilder sb = new StringBuilder();
            sb.append("\nDANH SÁCH SẢN PHẨM HIỆN CÓ (phân theo danh mục):\n");
            sb.append("LƯU Ý: Khi người dùng hỏi về 'rau' thì CHỈ lấy danh mục 'Rau lá'.\n");
            sb.append("Khi người dùng hỏi về 'củ' thì CHỈ lấy danh mục 'Củ'.\n");
            sb.append("Khi người dùng hỏi về 'quả/trái cây' thì CHỈ lấy danh mục 'Quả/Trái cây'.\n\n");

            byCategory.forEach((cat, names) -> {
                sb.append("[Danh mục: ").append(cat).append("]\n");
                // Mỗi tên trên 1 dòng để AI dễ đọc và trả lời đúng
                names.forEach(name -> sb.append("  - ").append(name).append("\n"));
                sb.append("\n");
            });

            sb.append("(Khi liệt kê sản phẩm, hãy dùng ĐÚNG tên như trên để hệ thống hiển thị link mua hàng)\n");

            return sb.toString();
        } catch (Exception e) {
            log.warn("Cannot load product catalog: {}", e.getMessage());
            return "";
        }
    }

}