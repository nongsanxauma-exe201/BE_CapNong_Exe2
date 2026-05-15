package com.swd301.foodmarket.dto.request;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
public class QualityCheckRequest {
    private String note;
    private com.swd301.foodmarket.enums.QualityResult qualityResult;
    private Integer orderDetailId;
    private List<QualityCheckingRequest> qualityChecks;
    @Data
    public static class QualityCheckingRequest{
        private Integer qualityCrieriaId;
        private boolean passed;

    }
}
