package com.swd301.foodmarket.service;

import com.swd301.foodmarket.dto.request.ProductCreationRequest;
import com.swd301.foodmarket.dto.request.ProductUpdateRequest;
import com.swd301.foodmarket.dto.response.PageResponse;
import com.swd301.foodmarket.dto.response.ProductResponse;
import com.swd301.foodmarket.entity.Product;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ProductService {
    ProductResponse createProduct(ProductCreationRequest request, MultipartFile image);

    ProductResponse  updateProduct(Integer id,ProductUpdateRequest request, MultipartFile image);

    void deleteProduct(Integer id);

    List<ProductResponse> getAll();

    PageResponse<ProductResponse> getAllPaged(int page, int size);

    // get by id
    ProductResponse getById(Integer id);

     List<ProductResponse> getByShopId(Integer shopId);

    PageResponse<ProductResponse> getByShopIdPaged(Integer shopId, int page, int size);

    List<ProductResponse> getPendingProducts();

    ProductResponse approveProduct(Integer id);

    ProductResponse rejectProduct(Integer id, String reason);
}
