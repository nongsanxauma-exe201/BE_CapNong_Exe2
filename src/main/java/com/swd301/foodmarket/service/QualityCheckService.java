package com.swd301.foodmarket.service;

import com.swd301.foodmarket.dto.request.QualityCheckRequest;
import com.swd301.foodmarket.dto.response.QualityCheckResponse;
import com.swd301.foodmarket.entity.QualityCriteria;
import org.springframework.data.domain.Page;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface QualityCheckService {

    QualityCheckResponse checkQuality(QualityCheckRequest qualityCheckRequest, MultiValueMap<String, MultipartFile> files);
    QualityCheckResponse getByOrderItemId(Integer orderItemId);
    QualityCheckResponse getByMysteryBox(Integer orderItemId);

    Page<QualityCriteria> getQualityCriteriaById(int page , int size);
}
