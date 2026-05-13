package com.swd301.foodmarket.service.impl;

import com.swd301.foodmarket.dto.request.BuildComboCreationRequest;
import com.swd301.foodmarket.dto.request.BuildComboUpdateRequest;
import com.swd301.foodmarket.dto.request.ProductComboRequest;
import com.swd301.foodmarket.dto.response.BuildComboResponse;
import com.swd301.foodmarket.dto.response.ShopComboCountResponse;
import com.swd301.foodmarket.entity.BuildCombo;
import com.swd301.foodmarket.entity.Product;
import com.swd301.foodmarket.entity.ProductCombo;
import com.swd301.foodmarket.entity.User;
import com.swd301.foodmarket.exception.AppException;
import com.swd301.foodmarket.exception.ErrorCode;
import com.swd301.foodmarket.mapper.BuildComboMapper;
import com.swd301.foodmarket.repository.BuildComboRepository;
import com.swd301.foodmarket.repository.ProductRepository;
import com.swd301.foodmarket.repository.UserRepository;
import com.swd301.foodmarket.service.BuildComboService;
import com.swd301.foodmarket.service.CloudinaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BuildComboServiceImpl implements BuildComboService {
    private final BuildComboRepository comboRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final BuildComboMapper comboMapper;
    private final CloudinaryService cloudinaryService;

    @Override
    public BuildComboResponse create(BuildComboCreationRequest request, MultipartFile image) {
        User shopOwner = getCurrentUser();

        BuildCombo combo = comboMapper.toEntity(request);
        combo.setShopOwner(shopOwner);

        if (image != null && !image.isEmpty()) {
            String imageUrl = cloudinaryService.uploadImage(image);
            combo.setImageUrl(imageUrl);
        }

        List<ProductCombo> items = request.getItems().stream().map(itemReq -> {

            Product product = productRepository.findById(itemReq.getProductId())
                    .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

            ProductCombo item = new ProductCombo();
            item.setCombo(combo);
            item.setProduct(product);
            item.setQuantity(itemReq.getQuantity());

            return item;
        }).toList();

        combo.setItems(items);


        BigDecimal total = items.stream()
                .map(i -> i.getProduct().getSellingPrice()
                        .multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);


        if (request.getDiscountPrice().compareTo(total) > 0) {
            throw new AppException(ErrorCode.INVALID_PRICE);
        }

        return comboMapper.toResponse(comboRepository.save(combo));
    }

    @Override
    public BuildComboResponse update(Integer id, BuildComboUpdateRequest request, MultipartFile image) {

        BuildCombo combo = comboRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.BUILD_COMBO_NOT_FOUND));

        if (!combo.getShopOwner().getId().equals(getCurrentUser().getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        if (image != null && !image.isEmpty()) {
            String imageUrl = cloudinaryService.uploadImage(image);
            combo.setImageUrl(imageUrl);
        }

        comboMapper.update(combo, request);

        if (request.getItems() != null) {

            BigDecimal totalPrice = BigDecimal.ZERO;
            List<ProductCombo> newItems = new ArrayList<>();

            for (ProductComboRequest itemReq : request.getItems()) {

                if (itemReq.getQuantity() <= 0) {
                    throw new AppException(ErrorCode.INVALID_QUANTITY);
                }

                Product product = productRepository.findById(itemReq.getProductId())
                        .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

                BigDecimal price = product.getSellingPrice();

                BigDecimal itemTotal = price.multiply(BigDecimal.valueOf(itemReq.getQuantity()));
                totalPrice = totalPrice.add(itemTotal);

                newItems.add(ProductCombo.builder()
                        .combo(combo)
                        .product(product)
                        .quantity(itemReq.getQuantity())
                        .build());
            }

            if (combo.getDiscountPrice().compareTo(totalPrice) > 0) {
                throw new AppException(ErrorCode.INVALID_COMBO_PRICE);
            }

            combo.getItems().clear();
            combo.getItems().addAll(newItems);
        }

        return comboMapper.toResponse(comboRepository.save(combo));
    }

    @Override
    public void delete(Integer id) {

        BuildCombo combo = comboRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.BUILD_COMBO_NOT_FOUND));

        if (!combo.getShopOwner().getId().equals(getCurrentUser().getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        comboRepository.delete(combo);
    }

    @Override
    public BuildComboResponse getById(Integer id) {
        return comboMapper.toResponse(
                comboRepository.findById(id)
                        .orElseThrow(() -> new AppException(ErrorCode.BUILD_COMBO_NOT_FOUND))
        );
    }

    @Override
    public List<BuildComboResponse> getAll() {
        return comboRepository.findAll()
                .stream()
                .map(comboMapper::toResponse)
                .toList();
    }


    @Override
    public List<BuildComboResponse> getByShopId(Integer shopId) {

        User shop = userRepository.findById(shopId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        return comboRepository.findByShopOwner_Id(shop.getId())
                .stream()
                .map(comboMapper::toResponse)
                .toList();
    }

    @Override
    public List<BuildComboResponse> getMyCombos() {

        User currentUser = getCurrentUser();

        return comboRepository.findByShopOwner_Id(currentUser.getId())
                .stream()
                .map(comboMapper::toResponse)
                .toList();
    }

    @Override
    public List<ShopComboCountResponse> getShopsByComboCount() {
        return comboRepository.findAll().stream()
                .collect(Collectors.groupingBy(BuildCombo::getShopOwner, Collectors.counting()))
                .entrySet().stream()
                .map(entry -> ShopComboCountResponse.builder()
                        .shopId(entry.getKey().getId())
                        .shopName(entry.getKey().getShopName())
                        .logoUrl(entry.getKey().getLogoUrl())
                        .comboCount(entry.getValue())
                        .build())
                .sorted((a, b) -> b.getComboCount().compareTo(a.getComboCount()))
                .toList();
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        String email = auth.getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }
}
