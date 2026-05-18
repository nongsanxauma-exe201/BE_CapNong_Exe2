package com.swd301.foodmarket.dto.response;

import com.swd301.foodmarket.dto.request.QualityCheckRequest;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class QualityCheckResponse {
    private String note;
    private com.swd301.foodmarket.enums.QualityResult qualityResult;
    private Integer orderDetailId;
    private List<QualityCheckingResponse> qualityCheckingResponses;
    private LocalDateTime createdAt;
    private double  rejectedWeight;


    @Data
    public static class QualityCheckingResponse {
        private String qualityName;
        private UUID qualityCheckId;
        private boolean isPassed;
        private List<ImageResponse> images;
    }
}
