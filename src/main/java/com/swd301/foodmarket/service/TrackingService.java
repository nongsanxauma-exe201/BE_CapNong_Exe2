package com.swd301.foodmarket.service;

import com.swd301.foodmarket.entity.Order;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface TrackingService {
   void trackingOrder(Order order, List<MultipartFile> files);
}
