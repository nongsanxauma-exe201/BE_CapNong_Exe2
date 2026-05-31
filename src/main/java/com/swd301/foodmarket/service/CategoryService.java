package com.swd301.foodmarket.service;

import com.swd301.foodmarket.dto.response.CategoryResponse;

import java.util.List;

public interface CategoryService {
    List<CategoryResponse> getAll();
}
