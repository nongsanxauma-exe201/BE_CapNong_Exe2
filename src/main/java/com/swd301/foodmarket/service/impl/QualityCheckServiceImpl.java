package com.swd301.foodmarket.service.impl;

import com.swd301.foodmarket.dto.request.QualityCheckRequest;
import com.swd301.foodmarket.dto.response.QualityCheckResponse;
import com.swd301.foodmarket.entity.*;
import com.swd301.foodmarket.exception.AppException;
import com.swd301.foodmarket.exception.ErrorCode;
import com.swd301.foodmarket.mapper.QualityResultMapper;
import com.swd301.foodmarket.repository.*;
import com.swd301.foodmarket.service.CloudinaryService;
import com.swd301.foodmarket.service.QcSettlementService;
import com.swd301.foodmarket.service.QualityCheckService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class QualityCheckServiceImpl implements QualityCheckService {
    private final QualityCrieriaRepository qualityCrieriaRepository;
    private final QualityResutlRepository qualityResutlRepository;
    private final QualityCheckRepository qualityCheckRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final ImageRepository imageRepository;
    private final CloudinaryService cloudinaryService;
    private final QualityResultMapper qualityResultMapper;
    private final OrderMyteryRepository orderMyteryRepository;
    private final QcSettlementService qcSettlementService;

    @Override
    @Transactional
    public QualityCheckResponse checkQuality(QualityCheckRequest qualityCheckRequest, MultiValueMap<String, MultipartFile> fileMap) {
        OrderDetail orderDetail = null;
        OrderMysteryBox mysteryBox = null;
        log.info("[QC_REQUEST] orderDetailId={}, orderMysteryBoxId={}, rejectedWeight={}",
                qualityCheckRequest.getOrderDetailId(),
                qualityCheckRequest.getOrderMysteryBoxId(),
                qualityCheckRequest.getRejectedWeight());

        if (qualityCheckRequest.getOrderDetailId() != null) {
            orderDetail = orderDetailRepository.findById(qualityCheckRequest.getOrderDetailId()).orElseThrow(
                    () -> new AppException(ErrorCode.ORDER_DETAIL_NOT_FOUND)
            );
        } else if (qualityCheckRequest.getOrderMysteryBoxId() != null) {
            mysteryBox = orderMyteryRepository.findById(qualityCheckRequest.getOrderMysteryBoxId()).orElseThrow(
                    () -> new AppException(ErrorCode.MYSTERY_BOX_NOT_FOUND)
            );
        } else {
            throw new AppException(ErrorCode.INVALID_QUALITY_CHECK_REQUEST);
        }

        BigDecimal rejectedKg = qualityCheckRequest.getRejectedKg() != null
                ? qualityCheckRequest.getRejectedKg()
                : BigDecimal.valueOf(qualityCheckRequest.getRejectedWeight());
        if (mysteryBox != null) {
            validateMysteryBoxRejectedQuantity(rejectedKg, mysteryBox);
        }
        BigDecimal requestedKg = orderDetail != null
                ? resolveOrderDetailKg(orderDetail)
                : resolveMysteryBoxKg(mysteryBox);

        String damageNote = normalizeText(qualityCheckRequest.getDamageNote());
        com.swd301.foodmarket.enums.QualityResult finalQualityResult = resolveFinalQualityResult(
                qualityCheckRequest, rejectedKg, damageNote);

        QualityResult qualityResult = QualityResult.builder()
                .note(qualityCheckRequest.getNote())
                .damageNote(damageNote)
                .qualityResult(finalQualityResult)
                .rejectedWeight(rejectedKg.doubleValue())
                .requestedKg(requestedKg)
                .approvedKg(qualityCheckRequest.getApprovedKg() != null
                        ? qualityCheckRequest.getApprovedKg()
                        : requestedKg.subtract(rejectedKg).max(BigDecimal.ZERO))
                .rejectedKg(rejectedKg)
                .build();

        Order order;
        if (mysteryBox != null) {
            qualityResult.setOrderMysteryBox(mysteryBox);
            mysteryBox.setQualityResult(qualityResult);
            order = mysteryBox.getOrder();
        } else {
            qualityResult.setOrderDetail(orderDetail);
            orderDetail.setQualityResult(qualityResult);
            order = orderDetail.getOrder();
        }

        qualityResutlRepository.saveAndFlush(qualityResult);

        int count = 1;
        if (qualityCheckRequest.getQualityChecks() != null) {
            for (QualityCheckRequest.QualityCheckingRequest qualityCheckingRequest : qualityCheckRequest.getQualityChecks()) {
                com.swd301.foodmarket.entity.QualityCheck qualityCheck = com.swd301.foodmarket.entity.QualityCheck.builder()
                        .qualityCriteria(qualityCrieriaRepository.findById(qualityCheckingRequest.getQualityCrieriaId()).orElseThrow(
                                () -> new AppException(ErrorCode.QUALITY_CRIERIA_NOT_FOUND)
                        ))
                        .qualityResult(qualityResult)
                        .isPassed(qualityCheckingRequest.isPassed())
                        .build();
                qualityCheckRepository.save(qualityCheck);
                String key = "qc_" + count;
                List<MultipartFile> files = fileMap.get(key);
                if (files != null && !files.isEmpty()) {
                    for (MultipartFile file : files) {
                        if (file == null || file.isEmpty()) {
                            continue;
                        }
                        String imageUrl = cloudinaryService.uploadImage(file);
                        imageRepository.save(
                                Image.builder()
                                        .imageUrl(imageUrl)
                                        .qualityCheck(qualityCheck)
                                        .build()
                        );
                    }
                }
                count++;
            }
        }

        qcSettlementService.applyLineQcResult(order, orderDetail, mysteryBox, qualityResult, rejectedKg, requestedKg);
        qualityResutlRepository.save(qualityResult);
        qcSettlementService.syncOrderAfterQcItem(order);

        return qualityResultMapper.toQualityCheckResponse(qualityResult);
    }

    private BigDecimal resolveOrderDetailKg(OrderDetail detail) {
        if (detail.getQuantityKg() != null) return detail.getQuantityKg();
        return BigDecimal.valueOf(detail.getQuantity() == null ? 0 : detail.getQuantity()).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal resolveMysteryBoxKg(OrderMysteryBox mysteryBox) {
        if (mysteryBox.getRescuedWeightKg() != null) return mysteryBox.getRescuedWeightKg();
        if (mysteryBox.getUnitWeightKg() != null && mysteryBox.getQuantity() != null) {
            return mysteryBox.getUnitWeightKg()
                    .multiply(BigDecimal.valueOf(mysteryBox.getQuantity()))
                    .setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(mysteryBox.getQuantity() == null ? 0 : mysteryBox.getQuantity()).setScale(2, RoundingMode.HALF_UP);
    }

    private void validateMysteryBoxRejectedQuantity(BigDecimal rejectedKg, OrderMysteryBox mysteryBox) {
        if (rejectedKg == null || rejectedKg.signum() <= 0) {
            return;
        }
        if (rejectedKg.stripTrailingZeros().scale() > 0) {
            throw new AppException(ErrorCode.INVALID_QUALITY_CHECK_REQUEST);
        }
        int orderedBags = mysteryBox.getQuantity() == null ? 0 : mysteryBox.getQuantity();
        if (rejectedKg.compareTo(BigDecimal.valueOf(orderedBags)) > 0) {
            throw new AppException(ErrorCode.INVALID_QUALITY_CHECK_REQUEST);
        }
    }

    private com.swd301.foodmarket.enums.QualityResult resolveFinalQualityResult(
            QualityCheckRequest request,
            BigDecimal rejectedKg,
            String damageNote
    ) {
        if (request.getQualityResult() != null) {
            return request.getQualityResult();
        }
        boolean hasFailedCriteria = request.getQualityChecks() != null
                && request.getQualityChecks().stream().anyMatch(item -> !item.isPassed());
        boolean hasRejectedKg = rejectedKg != null && rejectedKg.signum() > 0;
        return (damageNote != null || hasRejectedKg || hasFailedCriteria)
                ? com.swd301.foodmarket.enums.QualityResult.REJECTED
                : com.swd301.foodmarket.enums.QualityResult.APPROVED;
    }

    private String normalizeText(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        return value.trim();
    }

    @Override
    public QualityCheckResponse getByOrderItemId(Integer orderItemId) {
        OrderDetail orderDetail = orderDetailRepository.findById(orderItemId).orElseThrow(
                () -> new AppException(ErrorCode.ORDER_DETAIL_NOT_FOUND)
        );
        if (orderDetail.getQualityResult() == null) {
            throw new AppException(ErrorCode.QUALITY_CHECK_NOT_FOUND);
        }
        return qualityResultMapper.toQualityCheckResponse(orderDetail.getQualityResult());
    }

    @Override
    public QualityCheckResponse getByMysteryBox(Integer orderItemId) {
        OrderMysteryBox mysteryBox = orderMyteryRepository.findById(orderItemId).orElseThrow(
                () -> new AppException(ErrorCode.MYSTERY_BOX_NOT_FOUND)
        );
        if (mysteryBox.getQualityResult() == null) {
            throw new AppException(ErrorCode.QUALITY_CHECK_NOT_FOUND);
        }
        return qualityResultMapper.toQualityCheckResponse(mysteryBox.getQualityResult());
    }

    @Override
    public Page<QualityCriteria> getQualityCriteriaById(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return qualityCrieriaRepository.findAll(pageable);
    }
}
