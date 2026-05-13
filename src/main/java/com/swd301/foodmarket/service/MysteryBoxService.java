package com.swd301.foodmarket.service;

import com.swd301.foodmarket.dto.request.MysteryBoxCreationRequest;
import com.swd301.foodmarket.dto.request.MysteryBoxUpdateRequest;
import com.swd301.foodmarket.dto.response.MysteryBoxResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface MysteryBoxService {

    MysteryBoxResponse createMysteryBox(MysteryBoxCreationRequest request, MultipartFile image);

    MysteryBoxResponse updateMysteryBox(Integer id, MysteryBoxUpdateRequest request, MultipartFile image);

    void deleteMysteryBox(Integer id);

    MysteryBoxResponse getById(Integer id);

    List<MysteryBoxResponse> getMyMysteryBoxes();

    List<MysteryBoxResponse> getAll();
}