package com.swd301.foodmarket.service.impl;


import com.swd301.foodmarket.dto.request.MysteryBoxCreationRequest;
import com.swd301.foodmarket.dto.request.MysteryBoxUpdateRequest;
import com.swd301.foodmarket.dto.request.ProductMysteryRequest;
import com.swd301.foodmarket.dto.response.MysteryBoxResponse;
import com.swd301.foodmarket.entity.MysteryBox;
import com.swd301.foodmarket.entity.Product;
import com.swd301.foodmarket.entity.ProductMystery;
import com.swd301.foodmarket.entity.User;
import com.swd301.foodmarket.exception.AppException;
import com.swd301.foodmarket.exception.ErrorCode;
import com.swd301.foodmarket.mapper.MysteryBoxMapper;
import com.swd301.foodmarket.repository.MysteryBoxRepository;
import com.swd301.foodmarket.repository.ProductMysteryRepository;
import com.swd301.foodmarket.repository.ProductRepository;
import com.swd301.foodmarket.repository.UserRepository;
import com.swd301.foodmarket.service.CloudinaryService;
import com.swd301.foodmarket.service.MysteryBoxService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MysteryBoxServiceImpl implements MysteryBoxService {

    private final MysteryBoxRepository mysteryBoxRepository;
    private final MysteryBoxMapper mysteryBoxMapper;
    private final CloudinaryService cloudinaryService;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private  final ProductMysteryRepository productMysteryRepository;

    @Transactional
    @Override
    public MysteryBoxResponse createMysteryBox(MysteryBoxCreationRequest request, MultipartFile image) {

        User shopOwner = getCurrentUser();

        if (request.getProducts() == null || request.getProducts().isEmpty()) {
            throw new AppException(ErrorCode.PRODUCT_LIST_EMPTY);
        }

        MysteryBox box = mysteryBoxMapper.toEntity(request);
        box.setShopOwner(shopOwner);
        box.setIsActive(true);
        box.setTotalQuantity(request.getTotalQuantity());
        box.setSoldQuantity(0);

        // upload image
        if (image != null && !image.isEmpty()) {
            String imageUrl = cloudinaryService.uploadImage(image);
            box.setImageUrl(imageUrl);
        }

        mysteryBoxRepository.save(box);

        for (ProductMysteryRequest item : request.getProducts()) {

            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

            // check product thuộc shop
            if (!product.getShopOwner().getId().equals(shopOwner.getId())) {
                throw new AppException(ErrorCode.UNAUTHORIZED);
            }

            // check stock
            if (product.getStockQuantity() < item.getQuantity()) {
                throw new AppException(ErrorCode.OUT_OF_STOCK);
            }

            ProductMystery pm = ProductMystery.builder()
                    .product(product)
                    .mysteryBox(box)
                    .quantity(item.getQuantity())
                    .build();

            productMysteryRepository.save(pm);

        }

        return mysteryBoxMapper.toResponse(box);
    }

    @Transactional
    @Override
    public MysteryBoxResponse updateMysteryBox(Integer id,
                                               MysteryBoxUpdateRequest request,
                                               MultipartFile image) {

        User currentUser = getCurrentUser();

        MysteryBox box = mysteryBoxRepository.findByIdWithProducts(id)
                .orElseThrow(() -> new AppException(ErrorCode.MYSTERY_BOX_NOT_FOUND));

        if (!box.getShopOwner().getId().equals(currentUser.getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        // update thông tin cơ bản
        mysteryBoxMapper.update(box, request);
        box.setIsActive(request.isActive());

        // update image
        if (image != null && !image.isEmpty()) {
            String imageUrl = cloudinaryService.uploadImage(image);
            box.setImageUrl(imageUrl);
        }


        List<ProductMystery> oldItems =
                productMysteryRepository.findByMysteryBox_Id(id);

        for (ProductMystery oldItem : oldItems) {
            Product product = oldItem.getProduct();
            product.setStockQuantity(
                    product.getStockQuantity() + oldItem.getQuantity()
            );
            productRepository.save(product);
        }


        box.getProductMysteries().clear();
        mysteryBoxRepository.save(box);
        mysteryBoxRepository.flush();


        if (request.getProducts() != null && !request.getProducts().isEmpty()) {

            for (ProductMysteryRequest item : request.getProducts()) {

                Product product = productRepository.findById(item.getProductId())
                        .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

                // check product thuộc shop
                if (!product.getShopOwner().getId().equals(currentUser.getId())) {
                    throw new AppException(ErrorCode.UNAUTHORIZED);
                }

                // check stock
                if (product.getStockQuantity() < item.getQuantity()) {
                    throw new AppException(ErrorCode.OUT_OF_STOCK);
                }

                ProductMystery pm = ProductMystery.builder()
                        .product(product)
                        .mysteryBox(box)
                        .quantity(item.getQuantity())
                        .build();

                box.getProductMysteries().add(pm);

                // trừ stock
                product.setStockQuantity(
                        product.getStockQuantity() - item.getQuantity()
                );
                productRepository.save(product);
            }
        }

        mysteryBoxRepository.save(box);

        return mysteryBoxMapper.toResponse(box);
    }

    @Transactional
    @Override
    public void deleteMysteryBox(Integer id) {

        MysteryBox box = mysteryBoxRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.MYSTERY_BOX_NOT_FOUND));

        User currentUser = getCurrentUser();

        if (!box.getShopOwner().getId().equals(currentUser.getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        mysteryBoxRepository.delete(box);
    }

    @Override
    public MysteryBoxResponse getById(Integer id) {

        MysteryBox box = mysteryBoxRepository.findByIdWithProducts(id)
                .orElseThrow(() -> new AppException(ErrorCode.MYSTERY_BOX_NOT_FOUND));

        return mysteryBoxMapper.toResponse(box);
    }

    @Override
    public List<MysteryBoxResponse> getMyMysteryBoxes() {
        User user = getCurrentUser();
        return mysteryBoxRepository.findByShopOwner_Id(user.getId())
                .stream()
                .map(mysteryBoxMapper::toResponse)
                .toList();
    }

    @Override
    public List<MysteryBoxResponse> getAll() {
        return mysteryBoxRepository.findAll()
                .stream()
                .map(mysteryBoxMapper::toResponse)
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