package com.swd301.foodmarket.service.impl;

import com.swd301.foodmarket.entity.Category;
import com.swd301.foodmarket.repository.CategoryRepository;
import com.swd301.foodmarket.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;
    @Override
    public List<Category> getALlCategory() {
        return categoryRepository.findAll();
    }
}
