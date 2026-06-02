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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

        QualityResult qualityResult = QualityResult.builder()
                .note(qualityCheckRequest.getNote())
                .qualityResult(qualityCheckRequest.getQualityResult())
                .rejectedWeight(qualityCheckRequest.getRejectedWeight())
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
        // update status order
        updateOrderStatus(order);
        // update Reject quality
        Integer qualityCheckCount = orderDetail != null
                ? orderDetail.getQuantity()
                : mysteryBox.getQuantity();

        String message = "";
        BigDecimal refundAmount = BigDecimal.ZERO;

        if (qualityResult.getRejectedWeight() != null
                && qualityResult.getRejectedWeight() > 0) {

            double percentage =
                    (double) (qualityResult.getRejectedWeight() * 100) / qualityCheckCount;

            if (percentage > 5) {

                BigDecimal unitPrice;

                String itemName;

                if (orderDetail != null) {
                    unitPrice = orderDetail.getUnitPrice();
                    itemName = orderDetail.getProduct().getProductName();
                } else {
                    unitPrice = mysteryBox.getMysteryBox().getPrice();
                    itemName = mysteryBox.getMysteryBox().getDescription();
                }

                refundAmount = unitPrice.multiply(
                        BigDecimal.valueOf(qualityResult.getRejectedWeight())
                );

                String rejectedUnit = orderDetail != null ? "kg/số lượng" : "túi";

                message = "Sản phẩm " + itemName
                        + " bị hư hỏng " + qualityResult.getRejectedWeight() + " " + rejectedUnit
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
