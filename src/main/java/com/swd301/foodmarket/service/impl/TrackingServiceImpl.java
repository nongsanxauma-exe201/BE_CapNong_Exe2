package com.swd301.foodmarket.service.impl;

import com.swd301.foodmarket.entity.Order;
import com.swd301.foodmarket.entity.Tracking;
import com.swd301.foodmarket.repository.ImageRepository;
import com.swd301.foodmarket.repository.OrderRepository;
import com.swd301.foodmarket.repository.TrackingRepository;
import com.swd301.foodmarket.service.CloudinaryService;
import com.swd301.foodmarket.service.TrackingService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TrackingServiceImpl  implements TrackingService {
    private final TrackingRepository trackingRepository;
    private final ImageRepository imageRepository;
    private final CloudinaryService cloudinaryService;
    @Override
    @Transactional
    public void trackingOrder(Order order, List<MultipartFile> fileList) {
        Tracking tracking = Tracking.builder()
                .order(order)
                .status(order.getStatus())
                .build();
        trackingRepository.save(tracking);
        if(fileList != null && !fileList.isEmpty()){
            fileList.forEach(file -> {
                String imageUrl = cloudinaryService.uploadImage(file);
                imageRepository.save(com.swd301.foodmarket.entity.Image.builder()
                        .imageUrl(imageUrl)
                        .tracking(tracking)
                        .build());
            });
        }

    }
}
