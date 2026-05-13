package com.swd301.foodmarket.service.impl;

import com.swd301.foodmarket.dto.request.ProductCreationRequest;
import com.swd301.foodmarket.dto.request.ProductUpdateRequest;
import com.swd301.foodmarket.dto.response.PageResponse;
import com.swd301.foodmarket.dto.response.ProductResponse;
import com.swd301.foodmarket.entity.Category;
import com.swd301.foodmarket.entity.Product;
import com.swd301.foodmarket.entity.User;
import com.swd301.foodmarket.enums.ProductStatus;
import com.swd301.foodmarket.exception.AppException;
import com.swd301.foodmarket.exception.ErrorCode;
import com.swd301.foodmarket.mapper.ProductMapper;
import com.swd301.foodmarket.repository.CategoryRepository;
import com.swd301.foodmarket.repository.ProductRepository;
import com.swd301.foodmarket.repository.UserRepository;
import com.swd301.foodmarket.service.CloudinaryService;
import com.swd301.foodmarket.service.ProductService;
import com.swd301.foodmarket.service.UserService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ProductServiceImpl implements ProductService {
    private final ProductMapper productMapper;
    private ProductRepository productRepository;
    private UserRepository userRepository;
    private CategoryRepository categoryRepository;
    private final CloudinaryService cloudinaryService;

    @Override
    public ProductResponse createProduct(ProductCreationRequest request, MultipartFile image){

        Product product = productMapper.toProduct(request);

        if (image != null && !image.isEmpty()) {
            String imageUrl = cloudinaryService.uploadImage(image);
            product.setImageUrl(imageUrl);
        }

        product.setStatus(ProductStatus.PENDING);
        product.setShopOwner(getCurrentUser());

        Category category = categoryRepository.findById(
                Long.valueOf(request.getCategoryId())
        ).orElseThrow(() -> new RuntimeException("Category not found"));

        product.setCategory(category);

        return productMapper.toResponse(productRepository.save(product));
    }

    @Override
    public ProductResponse updateProduct(Integer id, ProductUpdateRequest request, MultipartFile image) {

        Product product = productRepository.findById(Integer.valueOf(id))
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        if (!product.getShopOwner().getId().equals(getCurrentUser().getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        productMapper.update(product, request);

        if (image != null && !image.isEmpty()) {
            String imageUrl = cloudinaryService.uploadImage(image);
            product.setImageUrl(imageUrl);
        }

        return productMapper.toResponse(productRepository.save(product));
    }

    @Override
    public void deleteProduct(Integer id) {
        Product product = productRepository.findById(Integer.valueOf(id))
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        if (!product.getShopOwner().getId().equals(getCurrentUser().getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        try {
            productRepository.delete(product);
        } catch (DataIntegrityViolationException e) {
            throw new AppException(ErrorCode.PRODUCT_IN_USE);
        }
    }

    @Override
    public List<ProductResponse> getAll() {
        return productRepository.findAll()
                .stream()
                .map(productMapper::toResponse)
                .toList();
    }

    @Override
    public PageResponse<ProductResponse> getAllPaged(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<Product> pageResult = productRepository.findAll(pageable);
        return PageResponse.<ProductResponse>builder()
                .content(pageResult.getContent().stream().map(productMapper::toResponse).toList())
                .page(pageResult.getNumber())
                .size(pageResult.getSize())
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .first(pageResult.isFirst())
                .last(pageResult.isLast())
                .build();
    }

    @Override
    public ProductResponse getById(Integer id) {
        Product product = productRepository.findById(Integer.valueOf(id))
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        return productMapper.toResponse(product);
    }

    @Override
    public List<ProductResponse> getByShopId(Integer shopId) {
        return productRepository.findByShopOwnerId(Integer.valueOf(shopId))
                .stream()
                .map(productMapper::toResponse)
                .toList();
    }

    @Override
    public PageResponse<ProductResponse> getByShopIdPaged(Integer shopId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<Product> pageResult = productRepository.findByShopOwnerId(shopId, pageable);
        return PageResponse.<ProductResponse>builder()
                .content(pageResult.getContent().stream().map(productMapper::toResponse).toList())
                .page(pageResult.getNumber())
                .size(pageResult.getSize())
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .first(pageResult.isFirst())
                .last(pageResult.isLast())
                .build();
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }

    @Override
    public List<ProductResponse> getPendingProducts() {
        return productRepository.findByStatus(ProductStatus.PENDING)
                .stream()
                .map(productMapper::toResponse)
                .toList();
    }

    @Override
    public ProductResponse approveProduct(Integer id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
        product.setStatus(ProductStatus.AVAILABLE);
        return productMapper.toResponse(productRepository.save(product));
    }

    @Override
    public ProductResponse rejectProduct(Integer id, String reason) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
        product.setStatus(ProductStatus.INACTIVE);
        if (reason != null && !reason.isBlank()) {
            String existing = product.getDescription() != null ? product.getDescription() : "";
            product.setDescription("[TỪ CHỐI: " + reason + "] " + existing);
        }
        return productMapper.toResponse(productRepository.save(product));
    }
}