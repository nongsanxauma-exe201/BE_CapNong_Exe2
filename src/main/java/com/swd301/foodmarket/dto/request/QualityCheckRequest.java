package com.swd301.foodmarket.dto.request;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

@Data
public class QualityCheckRequest {
    private String note;
    private com.swd301.foodmarket.enums.QualityResult qualityResult;
    private Integer orderDetailId; // neu la mau binh thuong nhu hien tai
    private Integer orderMysteryBoxId; // neu la mau mystery box
    private List<QualityCheckingRequest> qualityChecks;
    private double  rejectedWeight;
    private BigDecimal rejectedKg;
    private BigDecimal approvedKg;
    private BigDecimal actualKg;
    private String damageNote;

    @Data
    public static class QualityCheckingRequest{
        private Integer qualityCrieriaId;
        private boolean passed;

    }
}
