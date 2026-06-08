package com.swd301.foodmarket.service;

import com.swd301.foodmarket.dto.request.ProductCreationRequest;
import com.swd301.foodmarket.dto.request.ProductImageReorderRequest;
import com.swd301.foodmarket.dto.request.ProductUpdateRequest;
import com.swd301.foodmarket.dto.request.SetPrimaryImageRequest;
import com.swd301.foodmarket.dto.response.PageResponse;
import com.swd301.foodmarket.dto.response.ProductImageResponse;
import com.swd301.foodmarket.dto.response.ProductResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ProductService {
    ProductResponse createProduct(ProductCreationRequest request, List<MultipartFile> images);

    ProductResponse updateProduct(Integer id, ProductUpdateRequest request, MultipartFile image);

    void deleteProduct(Integer id);

    void activeProduct(Integer id);

    List<ProductResponse> getAll();

    PageResponse<ProductResponse> getAllPaged(int page, int size);

    ProductResponse getById(Integer id);

    List<ProductResponse> getByShopId(Integer shopId);

    PageResponse<ProductResponse> getByShopIdPaged(Integer shopId, int page, int size);

    List<ProductResponse> getPendingProducts();

    ProductResponse approveProduct(Integer id);

    ProductResponse rejectProduct(Integer id, String reason);

    List<ProductResponse> getProductForBuyer();

    // Gallery management
    List<ProductImageResponse> getImages(Integer productId);

    List<ProductImageResponse> addImages(Integer productId, List<MultipartFile> images);

    void deleteImage(Integer productId, Long imageId);

    ProductImageResponse setPrimaryImage(Integer productId, SetPrimaryImageRequest request);

    List<ProductImageResponse> reorderImages(Integer productId, ProductImageReorderRequest request);
}
