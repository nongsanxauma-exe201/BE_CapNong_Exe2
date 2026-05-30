package com.swd301.foodmarket.controller;

import com.swd301.foodmarket.dto.response.ApiResponse;
import com.swd301.foodmarket.entity.Category;
import com.swd301.foodmarket.repository.CategoryRepository;
import com.swd301.foodmarket.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategoryController {
    private final CategoryService categoryService;

    @GetMapping
    public ApiResponse<List<Category>> getAll() {
        return ApiResponse.<List<Category>>builder()
                .result(categoryService.getALlCategory())
                .build();
    }

}
