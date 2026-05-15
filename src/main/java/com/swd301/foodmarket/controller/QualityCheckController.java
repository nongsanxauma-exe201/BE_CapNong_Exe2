package com.swd301.foodmarket.controller;

import com.swd301.foodmarket.dto.request.QualityCheckRequest;
import com.swd301.foodmarket.dto.response.ApiResponse;
import com.swd301.foodmarket.dto.response.QualityCheckResponse;
import com.swd301.foodmarket.service.QualityCheckService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/quality-checks")
public class QualityCheckController {
    private final QualityCheckService qualityCheckService;


    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<QualityCheckResponse> createQualityCheck(
            @RequestPart QualityCheckRequest qualityCheckRequest,
            @RequestParam MultiValueMap<String, MultipartFile> fileMap

    ) {
        return ApiResponse.<QualityCheckResponse>builder()
                .result(qualityCheckService.checkQuality(qualityCheckRequest, fileMap))
                .build();
    }
    @GetMapping("/{orderDetailId}")
    public ApiResponse<QualityCheckResponse> getByOrderItemsId(
           @PathVariable Integer orderDetailId
    ) {
        return ApiResponse.<QualityCheckResponse>builder()
                .result(qualityCheckService.getByOrderItemId(orderDetailId))
                .build();
    }
    @GetMapping("/criteria")
    public ApiResponse<?> getQualityCriteria(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.builder()
                .result(qualityCheckService.getQualityCriteriaById(page, size))
                .build();
    }
}
