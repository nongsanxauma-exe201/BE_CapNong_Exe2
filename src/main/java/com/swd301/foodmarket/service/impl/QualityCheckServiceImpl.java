package com.swd301.foodmarket.service.impl;

import com.swd301.foodmarket.dto.request.OrderUpdateRequest;
import com.swd301.foodmarket.dto.request.QualityCheckRequest;
import com.swd301.foodmarket.dto.response.QualityCheckResponse;
import com.swd301.foodmarket.entity.*;
import com.swd301.foodmarket.enums.OrderStatus;
import com.swd301.foodmarket.exception.AppException;
import com.swd301.foodmarket.exception.ErrorCode;
import com.swd301.foodmarket.mapper.QualityResultMapper;
import com.swd301.foodmarket.repository.*;
import com.swd301.foodmarket.service.CloudinaryService;
import com.swd301.foodmarket.service.NotificationService;
import com.swd301.foodmarket.service.OrderService;
import com.swd301.foodmarket.service.QualityCheckService;
import com.swd301.foodmarket.service.VoucherService;
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
    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final NotificationService notificationService;
    private final MysteryBoxRepository mysteryBoxRepository;
    private final OrderMyteryRepository orderMyteryRepository;
    private final VoucherService voucherService;

    @Override
    @Transactional
    public QualityCheckResponse checkQuality(QualityCheckRequest qualityCheckRequest, MultiValueMap<String, MultipartFile> fileMap) {
        OrderDetail orderDetail = null;
        OrderMysteryBox mysteryBox = null;
        log.info("[QC_REQUEST] orderDetailId={}, orderMysteryBoxId={}, qualityResult={}, rejectedWeight={}"                , qualityCheckRequest.getOrderDetailId()
                , qualityCheckRequest.getOrderMysteryBoxId()
                , qualityCheckRequest.getQualityResult()
                , qualityCheckRequest.getRejectedWeight()
        );
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
        BigDecimal requestedKg = orderDetail != null
                ? resolveOrderDetailKg(orderDetail)
                : resolveMysteryBoxKg(mysteryBox);

        String damageNote = normalizeText(qualityCheckRequest.getDamageNote());
        com.swd301.foodmarket.enums.QualityResult finalQualityResult = resolveFinalQualityResult(qualityCheckRequest, rejectedKg, damageNote);

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
                .refundedKg(rejectedKg)
                .build();
        Order order = null;
        if (mysteryBox != null) {
            qualityResult.setOrderMysteryBox(mysteryBox);
            mysteryBox.setQualityResult(qualityResult); // thêm dòng này
            order = mysteryBox.getOrder();
        } else if (orderDetail != null) {
            qualityResult.setOrderDetail(orderDetail);
            orderDetail.setQualityResult(qualityResult); // nên thêm luôn cho product
            order = orderDetail.getOrder();
        }
        qualityResutlRepository.saveAndFlush(qualityResult);
        int count = 1;
        if (qualityCheckRequest.getQualityChecks() != null) for (QualityCheckRequest.QualityCheckingRequest qualityCheckingRequest : qualityCheckRequest.getQualityChecks()) {
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

            log.info(
                    "Processing quality check {}: Found {} files for key '{}'",
                    count,
                    files != null ? files.size() : 0,
                    key
            );

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
        if (isRejectedOverDamageVoucherThreshold(rejectedKg, requestedKg)) {
            voucherService.createAutoVoucherForDamage(order, buildDamageVoucherReason(rejectedKg, requestedKg));
            qualityResult.setAutoVoucherCreated(true);
            qualityResutlRepository.save(qualityResult);
        }
        // update status order
        updateOrderStatus(order);
        // update Reject quality
        String message = "";
        BigDecimal refundAmount = BigDecimal.ZERO;

        if (qualityResult.getRejectedKg() != null
                && qualityResult.getRejectedKg().signum() > 0) {

            BigDecimal percentage = requestedKg.signum() > 0
                    ? qualityResult.getRejectedKg().multiply(BigDecimal.valueOf(100)).divide(requestedKg, 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            if (percentage.compareTo(BigDecimal.valueOf(5)) > 0) {

                BigDecimal unitPrice;

                String itemName;

                if (orderDetail != null) {
                    unitPrice = orderDetail.getPricePerKg() != null ? orderDetail.getPricePerKg() : orderDetail.getUnitPrice();
                    itemName = orderDetail.getProduct().getProductName();
                } else {
                    unitPrice = mysteryBox.getMysteryBox().getPrice();
                    itemName = mysteryBox.getMysteryBox().getDescription();
                }

                refundAmount = unitPrice.multiply(
                        qualityResult.getRejectedKg()
                ).setScale(0, RoundingMode.HALF_UP);

                message = "Sản phẩm " + itemName
                        + " bị hư hỏng " + qualityResult.getRejectedKg().stripTrailingZeros().toPlainString() + " kg"
                        + ", phải hoàn " + refundAmount + "đ";
            }
        }
        BigDecimal currentRefund = order.getRefundAmount() != null
                ? order.getRefundAmount()
                : BigDecimal.ZERO;
        order.setRefundAmount(currentRefund.add(refundAmount));
        if ("COD".equalsIgnoreCase(order.getPaymentMethod())) {
            BigDecimal totalAmount = order.getTotalAmount() != null
                    ? order.getTotalAmount()
                    : BigDecimal.ZERO;

            BigDecimal newRefundAmount = order.getRefundAmount() != null
                    ? order.getRefundAmount()
                    : BigDecimal.ZERO;

            BigDecimal codAmount = totalAmount.subtract(newRefundAmount);

            if (codAmount.compareTo(BigDecimal.ZERO) < 0) {
                throw new AppException(ErrorCode.INVALID_AMOUNT);
            }

            order.setCodAmount(codAmount);
        }
        order.setRefundReason(order.getRefundReason() == null ? message : order.getRefundReason() + "\n" + message);
        orderRepository.save(order);
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

    private boolean isRejectedOverDamageVoucherThreshold(BigDecimal rejectedKg, BigDecimal requestedKg) {
        if (rejectedKg == null || requestedKg == null || requestedKg.signum() <= 0) {
            return false;
        }
        BigDecimal rejectedPercent = rejectedKg
                .multiply(BigDecimal.valueOf(100))
                .divide(requestedKg, 2, RoundingMode.HALF_UP);
        return rejectedPercent.compareTo(BigDecimal.valueOf(20)) > 0;
    }

    private String buildDamageVoucherReason(BigDecimal rejectedKg, BigDecimal requestedKg) {
        BigDecimal rejectedPercent = rejectedKg
                .multiply(BigDecimal.valueOf(100))
                .divide(requestedKg, 2, RoundingMode.HALF_UP);
        return "QC ghi nhận khối lượng không đạt chuẩn "
                + rejectedKg.stripTrailingZeros().toPlainString() + " kg / "
                + requestedKg.stripTrailingZeros().toPlainString() + " kg ("
                + rejectedPercent.stripTrailingZeros().toPlainString() + "%), vượt ngưỡng 20%";
    }

    // update status order
    public void updateOrderStatus(Order order) {
        int totalProductItems = order.getOrderDetails() != null
                ? order.getOrderDetails().size()
                : 0;

        int totalMysteryItems = order.getMysteryBoxes() != null
                ? order.getMysteryBoxes().size()
                : 0;

        int totalItems = totalProductItems + totalMysteryItems;

        int uncheckedProductItems = order.getOrderDetails() == null
                ? 0
                : (int) order.getOrderDetails()
                .stream()
                .filter(item -> item.getQualityResult() == null)
                .count();

        int uncheckedMysteryItems = order.getMysteryBoxes() == null
                ? 0
                : (int) order.getMysteryBoxes()
                .stream()
                .filter(item -> item.getQualityResult() == null)
                .count();

        int uncheckedItems = uncheckedProductItems + uncheckedMysteryItems;

        log.info(
                "Order {} quality status: totalItems={}, uncheckedProductItems={}, uncheckedMysteryItems={}, uncheckedItems={}",
                order.getId(),
                totalItems,
                uncheckedProductItems,
                uncheckedMysteryItems,
                uncheckedItems
        );

        if (totalItems <= 0) {
            return;
        }

        if (uncheckedItems == 0) {
            orderService.updateOrder(
                    order.getId(),
                    OrderUpdateRequest.builder()
                            .status(OrderStatus.QUALITY_CHECKED)
                            .build(),
                    null
            );
            return;
        }

        if (order.getStatus().equals(OrderStatus.ARRIVED_AT_WAREHOUSE)) {
            orderService.updateOrder(
                    order.getId(),
                    OrderUpdateRequest.builder()
                            .status(OrderStatus.QUALITY_CHECKING)
                            .build(),
                    null
            );
        }
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
