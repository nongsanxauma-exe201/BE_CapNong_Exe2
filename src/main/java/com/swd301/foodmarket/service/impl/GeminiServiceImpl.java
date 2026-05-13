package com.swd301.foodmarket.service.impl;

import com.google.genai.Client;
import com.google.genai.types.*;
import com.swd301.foodmarket.service.GeminiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.swd301.foodmarket.exception.AppException;
import com.swd301.foodmarket.exception.ErrorCode;

import java.util.Collections;

@Service
@Slf4j
public class GeminiServiceImpl implements GeminiService {

    @Value("${gemini.api.key}")
    private String serverApiKey;

    private static final String MODEL_NAME = "gemini-2.5-flash";

    /**
     * Gọi Gemini AI. Ưu tiên userApiKey nếu có, fallback về serverApiKey.
     */
    @Override
    public String generateResponse(String prompt, String userApiKey) {
        String apiKey = (userApiKey != null && !userApiKey.isBlank())
                ? userApiKey
                : serverApiKey;

        if (apiKey == null || apiKey.isBlank()) {
            throw new AppException(ErrorCode.GEMINI_API_KEY_MISSING);
        }

        try {
            Client client = Client.builder()
                    .apiKey(apiKey)
                    .build();

            Content content = Content.builder()
                    .parts(Collections.singletonList(Part.fromText(prompt.trim())))
                    .build();

            GenerateContentConfig config = GenerateContentConfig.builder()
                    .temperature(0.3f) // Ít sáng tạo, trả lời chính xác hơn
                    .build();

            GenerateContentResponse response = client.models.generateContent(
                    MODEL_NAME, content, config
            );

            if (response == null || response.text() == null) {
                throw new AppException(ErrorCode.GEMINI_API_ERROR);
            }

            return response.text();

        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("Gemini API call failed: {}", e.getMessage());
            throw new AppException(ErrorCode.GEMINI_API_ERROR);
        }
    }
}