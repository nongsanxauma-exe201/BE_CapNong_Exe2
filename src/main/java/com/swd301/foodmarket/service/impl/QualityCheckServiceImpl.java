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
    @Override
    @Transactional
    public QualityCheckResponse checkQuality(QualityCheckRequest qualityCheckRequest, MultiValueMap<String, MultipartFile> fileMap ) {
        OrderDetail orderDetail = orderDetailRepository.findById(qualityCheckRequest.getOrderDetailId()).orElseThrow(
                () -> new AppException(ErrorCode.ORDER_DETAIL_NOT_FOUND)
        );
        QualityResult qualityResult = QualityResult.builder()
                .note(qualityCheckRequest.getNote())
                .qualityResult(qualityCheckRequest.getQualityResult())
                .orderDetail(orderDetail)
                .build();
        qualityResutlRepository.save(qualityResult);
        int  count = 1;
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
        updateOrderStatus(orderDetail);
        return qualityResultMapper.toQualityCheckResponse(qualityResult);
    }
    // update status order
    public void updateOrderStatus(OrderDetail orderDetail){
        Order order = orderDetail.getOrder();
        if(order.getStatus().equals(OrderStatus.ARRIVED_AT_WAREHOUSE) && order.getOrderDetails().size() > 1)
        {
            orderService.updateOrder(order.getId(), OrderUpdateRequest.builder().status(OrderStatus.QUALITY_CHECKING).build(), null);
        }
        else
        {
            Integer totalItems = order.getOrderDetails().size();
            Integer checkedItems = orderDetailRepository.countByOrderAndQualityResultNull(order);
            log.info("Total items: {}, Checked items: {}", totalItems, checkedItems);
            if (totalItems == checkedItems)
            {
                orderService.updateOrder(order.getId(), OrderUpdateRequest.builder().status(OrderStatus.QUALITY_CHECKED).build(), null);
            }
        }

    }
    @Override
    public QualityCheckResponse getByOrderItemId(Integer orderItemId) {
        OrderDetail orderDetail = orderDetailRepository.findById(orderItemId).orElseThrow(
                () -> new AppException(ErrorCode.ORDER_DETAIL_NOT_FOUND)
        );
        if(orderDetail.getQualityResult() == null){
           throw new AppException(ErrorCode.QUALITY_CHECK_NOT_FOUND);
        }
        return qualityResultMapper.toQualityCheckResponse(orderDetail.getQualityResult());
    }

    @Override
    public Page<QualityCriteria> getQualityCriteriaById(int page, int size) {
        Pageable pageable = PageRequest.of(page,size);
        return qualityCrieriaRepository.findAll(pageable);
    }
}
