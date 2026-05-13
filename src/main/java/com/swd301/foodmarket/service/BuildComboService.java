package com.swd301.foodmarket.service;

import com.swd301.foodmarket.dto.request.BuildComboCreationRequest;
import com.swd301.foodmarket.dto.request.BuildComboUpdateRequest;
import com.swd301.foodmarket.dto.response.BuildComboResponse;
import com.swd301.foodmarket.dto.response.ShopComboCountResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface BuildComboService {
    BuildComboResponse create(BuildComboCreationRequest request, MultipartFile image);
    BuildComboResponse update(Integer id, BuildComboUpdateRequest request, MultipartFile image);

    void delete(Integer id);

    BuildComboResponse getById(Integer id);

    List<BuildComboResponse> getAll();

    List<BuildComboResponse> getByShopId(Integer shopId);

    List<BuildComboResponse> getMyCombos();

    List<ShopComboCountResponse> getShopsByComboCount();
}
