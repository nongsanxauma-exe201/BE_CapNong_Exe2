package com.swd301.foodmarket.dto.response;

import com.swd301.foodmarket.dto.request.QualityCheckRequest;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class QualityCheckResponse {
    private String note;
    private String damageNote;
    private boolean autoVoucherCreated;
    private com.swd301.foodmarket.enums.QualityResult qualityResult;
    private Integer orderDetailId;
    private Integer orderMysteryBoxId;
    private List<QualityCheckingResponse> qualityCheckingResponses;
    private LocalDateTime createdAt;
    private double  rejectedWeight;
    private BigDecimal requestedKg;
    private BigDecimal approvedKg;
    private BigDecimal rejectedKg;
    private BigDecimal refundedKg;
    private String damageTier;
    private boolean lineCancelled;
    private BigDecimal lineRefundAmount;


    @Data
    public static class QualityCheckingResponse {
        private String qualityName;
        private UUID qualityCheckId;
        private boolean isPassed;
        private List<ImageResponse> images;
    }
}
