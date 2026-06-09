package com.swd301.foodmarket.service.impl;

import com.swd301.foodmarket.dto.request.ProductCreationRequest;
import com.swd301.foodmarket.dto.request.ProductImageReorderRequest;
import com.swd301.foodmarket.dto.request.ProductUpdateRequest;
import com.swd301.foodmarket.dto.request.SetPrimaryImageRequest;
import com.swd301.foodmarket.dto.response.PageResponse;
import com.swd301.foodmarket.dto.response.ProductImageResponse;
import com.swd301.foodmarket.dto.response.ProductResponse;
import com.swd301.foodmarket.entity.Category;
import com.swd301.foodmarket.entity.Notification;
import com.swd301.foodmarket.entity.Product;
import com.swd301.foodmarket.entity.ProductImage;
import com.swd301.foodmarket.entity.User;
import com.swd301.foodmarket.enums.OrderStatus;
import com.swd301.foodmarket.enums.ProductStatus;
import com.swd301.foodmarket.exception.AppException;
import com.swd301.foodmarket.exception.ErrorCode;
import com.swd301.foodmarket.mapper.ProductMapper;
import com.swd301.foodmarket.repository.*;
import com.swd301.foodmarket.service.CloudinaryService;
import com.swd301.foodmarket.service.NotificationService;
import com.swd301.foodmarket.service.ProductService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final NotificationService notificationService;
    private final ProductImageRepository productImageRepository;

    @Override
    @Transactional
    public ProductResponse createProduct(ProductCreationRequest request, List<MultipartFile> images) {
        Product product = productMapper.toProduct(request);
        product.setStatus(ProductStatus.PENDING);
        product.setShopOwner(getCurrentUser());

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found"));
        product.setCategory(category);

        Product saved = productRepository.save(product);

        if (images != null && !images.isEmpty()) {
            if (images.size() > 8) throw new AppException(ErrorCode.MAX_IMAGES_EXCEEDED);

            List<ProductImage> productImages = new ArrayList<>();
            for (int i = 0; i < images.size(); i++) {
                MultipartFile file = images.get(i);
                if (file == null || file.isEmpty()) continue;
                Map<String, String> details = cloudinaryService.uploadImageAndGetDetails(file);
                boolean isPrimary = (i == 0);
                ProductImage img = ProductImage.builder()
                        .product(saved)
                        .imageUrl(details.get("url"))
                        .publicId(details.get("publicId"))
                        .isPrimary(isPrimary)
                        .displayOrder(i)
                        .build();
                productImages.add(img);
            }
            productImageRepository.saveAll(productImages);

            // Keep imageUrl in sync with primary image for backward compat
            if (!productImages.isEmpty()) {
                saved.setImageUrl(productImages.get(0).getImageUrl());
                productRepository.save(saved);
            }
        }

        return productMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public ProductResponse updateProduct(Integer id, ProductUpdateRequest request, MultipartFile image) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        if (!product.getShopOwner().getId().equals(getCurrentUser().getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found"));

        productMapper.update(product, request);
        product.setCategory(category);

        if (image != null && !image.isEmpty()) {
            Map<String, String> details = cloudinaryService.uploadImageAndGetDetails(image);
            product.setImageUrl(details.get("url"));

            // Replace or set primary image in gallery
            productImageRepository.findByProductIdAndIsPrimaryTrue(id).ifPresentOrElse(
                    existing -> {
                        existing.setImageUrl(details.get("url"));
                        existing.setPublicId(details.get("publicId"));
                        productImageRepository.save(existing);
                    },
                    () -> {
                        ProductImage img = ProductImage.builder()
                                .product(product)
                                .imageUrl(details.get("url"))
                                .publicId(details.get("publicId"))
                                .isPrimary(true)
                                .displayOrder(0)
                                .build();
                        productImageRepository.save(img);
                    }
            );
        }

        return productMapper.toResponse(productRepository.save(product));
    }

    @Override
    public void deleteProduct(Integer id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        if (!product.getShopOwner().getId().equals(getCurrentUser().getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        product.setStatus(ProductStatus.INACTIVE);
        productRepository.save(product);
    }

    @Override
    public void activeProduct(Integer id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        if (!product.getShopOwner().getId().equals(getCurrentUser().getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        product.setStatus(ProductStatus.AVAILABLE);
        productRepository.save(product);
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
        Page<Product> pageResult = productRepository.findAllByStatus(ProductStatus.AVAILABLE, pageable);
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
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        ProductResponse response = productMapper.toResponse(product);

        // Load gallery images
        List<ProductImage> images = productImageRepository.findByProductIdOrderByDisplayOrderAsc(id);
        if (!images.isEmpty()) {
            response.setImages(toImageResponseList(images));
        } else if (product.getImageUrl() != null) {
            // Fallback: wrap legacy imageUrl as single gallery item
            response.setImages(List.of(ProductImageResponse.builder()
                    .id(0L)
                    .imageUrl(product.getImageUrl())
                    .isPrimary(true)
                    .displayOrder(0)
                    .build()));
        }

        return response;
    }

    @Override
    public List<ProductResponse> getByShopId(Integer shopId) {
        return productRepository.findByShopOwnerId(shopId)
                .stream()
                .map(productMapper::toResponse)
                .toList();
    }

    @Override
    public PageResponse<ProductResponse> getByShopIdPaged(Integer shopId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<Product> pageResult = productRepository.findByShopOwnerId(shopId, pageable);
        List<ProductResponse> productResponses = new ArrayList<>();
        for (Product p : pageResult.getContent()) {
            ProductResponse pr = productMapper.toResponse(p);
            pr.setSold(orderDetailRepository.calculateSold(p.getId(), OrderStatus.DELIVERED));
            productResponses.add(pr);
        }
        return PageResponse.<ProductResponse>builder()
                .content(productResponses)
                .page(pageResult.getNumber())
                .size(pageResult.getSize())
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .first(pageResult.isFirst())
                .last(pageResult.isLast())
                .build();
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
        ProductResponse response = productMapper.toResponse(productRepository.save(product));

        User shopOwner = product.getShopOwner();
        if (shopOwner != null) {
            Notification notification = Notification.builder()
                    .admin(getCurrentUser())
                    .title("Sản phẩm đã được duyệt")
                    .message("Sản phẩm \"" + product.getProductName() + "\" của bạn đã được quản trị viên duyệt và đang được bày bán.")
                    .receiverType("SHOP_OWNER")
                    .build();
            notificationService.sendNotificationWebsocket(shopOwner, notification);
        }
        return response;
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
        ProductResponse response = productMapper.toResponse(productRepository.save(product));

        User shopOwner = product.getShopOwner();
        if (shopOwner != null) {
            String msg = reason != null && !reason.isBlank()
                    ? "Sản phẩm \"" + product.getProductName() + "\" bị từ chối. Lý do: " + reason
                    : "Sản phẩm \"" + product.getProductName() + "\" của bạn đã bị từ chối bởi quản trị viên.";
            Notification notification = Notification.builder()
                    .admin(getCurrentUser())
                    .title("Sản phẩm bị từ chối")
                    .message(msg)
                    .receiverType("SHOP_OWNER")
                    .build();
            notificationService.sendNotificationWebsocket(shopOwner, notification);
        }
        return response;
    }

    @Override
    public List<ProductResponse> getProductForBuyer() {
        return productRepository.findAllByStatusAndShopOwner_ShopActive(ProductStatus.AVAILABLE, true)
                .stream()
                .map(productMapper::toResponse)
                .toList();
    }

    // ─── Gallery methods ──────────────────────────────────────────────────────

    @Override
    public List<ProductImageResponse> getImages(Integer productId) {
        productRepository.findById(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
        return toImageResponseList(productImageRepository.findByProductIdOrderByDisplayOrderAsc(productId));
    }

    @Override
    @Transactional
    public List<ProductImageResponse> addImages(Integer productId, List<MultipartFile> images) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        verifyOwner(product);

        int existing = productImageRepository.countByProductId(productId);
        if (existing + images.size() > 8) throw new AppException(ErrorCode.MAX_IMAGES_EXCEEDED);

        List<ProductImage> saved = new ArrayList<>();
        int orderOffset = existing;
        for (MultipartFile file : images) {
            if (file == null || file.isEmpty()) continue;
            Map<String, String> details = cloudinaryService.uploadImageAndGetDetails(file);
            ProductImage img = ProductImage.builder()
                    .product(product)
                    .imageUrl(details.get("url"))
                    .publicId(details.get("publicId"))
                    .isPrimary(existing == 0 && saved.isEmpty())
                    .displayOrder(orderOffset++)
                    .build();
            saved.add(productImageRepository.save(img));
        }

        // Sync imageUrl with primary if first image was just added
        if (existing == 0 && !saved.isEmpty()) {
            product.setImageUrl(saved.get(0).getImageUrl());
            productRepository.save(product);
        }

        return toImageResponseList(productImageRepository.findByProductIdOrderByDisplayOrderAsc(productId));
    }

    @Override
    @Transactional
    public void deleteImage(Integer productId, Long imageId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        verifyOwner(product);

        ProductImage image = productImageRepository.findByIdAndProductId(imageId, productId)
                .orElseThrow(() -> new AppException(ErrorCode.IMAGE_NOT_FOUND));

        int count = productImageRepository.countByProductId(productId);
        if (count <= 1) throw new AppException(ErrorCode.MIN_IMAGES_REQUIRED);
        if (Boolean.TRUE.equals(image.getIsPrimary())) throw new AppException(ErrorCode.CANNOT_DELETE_PRIMARY_IMAGE);

        cloudinaryService.deleteImage(image.getPublicId());
        productImageRepository.delete(image);
    }

    @Override
    @Transactional
    public ProductImageResponse setPrimaryImage(Integer productId, SetPrimaryImageRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        verifyOwner(product);

        ProductImage newPrimary = productImageRepository.findByIdAndProductId(request.getImageId(), productId)
                .orElseThrow(() -> new AppException(ErrorCode.IMAGE_NOT_FOUND));

        // Clear all primary flags then set the new one
        List<ProductImage> allImages = productImageRepository.findByProductIdOrderByDisplayOrderAsc(productId);
        allImages.forEach(img -> img.setIsPrimary(false));
        productImageRepository.saveAll(allImages);

        newPrimary.setIsPrimary(true);
        ProductImage saved = productImageRepository.save(newPrimary);

        // Keep product.imageUrl in sync
        product.setImageUrl(saved.getImageUrl());
        productRepository.save(product);

        return toImageResponse(saved);
    }

    @Override
    @Transactional
    public List<ProductImageResponse> reorderImages(Integer productId, ProductImageReorderRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        verifyOwner(product);

        for (ProductImageReorderRequest.ImageOrderItem item : request.getImages()) {
            productImageRepository.findByIdAndProductId(item.getId(), productId).ifPresent(img -> {
                img.setDisplayOrder(item.getDisplayOrder());
                productImageRepository.save(img);
            });
        }

        return toImageResponseList(productImageRepository.findByProductIdOrderByDisplayOrderAsc(productId));
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }

    private void verifyOwner(Product product) {
        if (!product.getShopOwner().getId().equals(getCurrentUser().getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
    }

    private ProductImageResponse toImageResponse(ProductImage img) {
        return ProductImageResponse.builder()
                .id(img.getId())
                .imageUrl(img.getImageUrl())
                .isPrimary(img.getIsPrimary())
                .displayOrder(img.getDisplayOrder())
                .build();
    }

    private List<ProductImageResponse> toImageResponseList(List<ProductImage> images) {
        return images.stream().map(this::toImageResponse).toList();
    }
}
