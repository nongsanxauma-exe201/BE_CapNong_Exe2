package com.swd301.foodmarket.controller;

import com.swd301.foodmarket.dto.request.BotKnowledgeRequest;
import com.swd301.foodmarket.dto.response.ApiResponse;
import com.swd301.foodmarket.dto.response.BotKnowledgeResponse;
import com.swd301.foodmarket.enums.KnowledgeType;
import com.swd301.foodmarket.service.BotKnowledgeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/bot-knowledge")
@RequiredArgsConstructor
@Slf4j
public class BotKnowledgeController {

    private final BotKnowledgeService botKnowledgeService;

    /** Thêm knowledge mới – ADMIN only */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<BotKnowledgeResponse> create(
            @Valid @RequestBody BotKnowledgeRequest request) {
        return ApiResponse.<BotKnowledgeResponse>builder()
                .result(botKnowledgeService.create(request))
                .message("Knowledge created successfully")
                .build();
    }

    /** Cập nhật knowledge – ADMIN only */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<BotKnowledgeResponse> update(
            @PathVariable Integer id,
            @Valid @RequestBody BotKnowledgeRequest request) {
        return ApiResponse.<BotKnowledgeResponse>builder()
                .result(botKnowledgeService.update(id, request))
                .message("Knowledge updated successfully")
                .build();
    }

    /** Xóa knowledge – ADMIN only */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable Integer id) {
        botKnowledgeService.delete(id);
        return ApiResponse.<Void>builder()
                .message("Knowledge deleted successfully")
                .build();
    }

    /** Xem chi tiết 1 knowledge – ADMIN only */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<BotKnowledgeResponse> getById(@PathVariable Integer id) {
        return ApiResponse.<BotKnowledgeResponse>builder()
                .result(botKnowledgeService.getById(id))
                .message("Knowledge retrieved successfully")
                .build();
    }

    /** Xem toàn bộ knowledge – ADMIN only */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<BotKnowledgeResponse>> getAll() {
        return ApiResponse.<List<BotKnowledgeResponse>>builder()
                .result(botKnowledgeService.getAll())
                .message("Knowledge list retrieved successfully")
                .build();
    }

    /** Lọc knowledge theo type – ADMIN only */
    @GetMapping("/type/{type}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<BotKnowledgeResponse>> getByType(
            @PathVariable KnowledgeType type) {
        return ApiResponse.<List<BotKnowledgeResponse>>builder()
                .result(botKnowledgeService.getByType(type))
                .message("Knowledge by type retrieved successfully")
                .build();
    }
}