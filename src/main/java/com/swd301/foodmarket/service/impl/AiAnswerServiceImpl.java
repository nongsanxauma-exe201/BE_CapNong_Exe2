package com.swd301.foodmarket.service.impl;

import com.swd301.foodmarket.service.AiAnswerService;
import com.swd301.foodmarket.service.GeminiService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiAnswerServiceImpl implements AiAnswerService {

    private final GeminiService geminiService;

    @Override
    public String answerWithContext(String question, String context) {
        if (context == null || context.isBlank()) {
            return "Mình chưa tìm thấy thông tin phù hợp trong dữ liệu hiện tại.";
        }

        String prompt = """
                Bạn là trợ lý ảo của Food Market - nền tảng thương mại điện tử chuyên về rau, củ, quả tươi sạch.
                Trả lời câu hỏi của khách hàng dựa trên tài liệu tham khảo bên dưới.
                Trả lời ngắn gọn, lịch sự bằng tiếng Việt.
                Nếu khách hỏi về sản phẩm đang bán, hãy liệt kê từ DANH SÁCH SẢN PHẨM nếu có.
                Nếu tài liệu không đủ thông tin, hãy nói rõ và không bịa đặt.

                TÀI LIỆU THAM KHẢO:
                %s

                CÂU HỎI: %s
                """.formatted(context.trim(), question.trim());

        return geminiService.generateResponse(prompt, null);
    }
}
