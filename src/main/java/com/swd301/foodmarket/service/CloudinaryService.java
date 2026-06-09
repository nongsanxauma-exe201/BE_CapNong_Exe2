package com.swd301.foodmarket.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface CloudinaryService {
    String uploadImage(MultipartFile file);
    // Returns {"url": ..., "publicId": ...}
    Map<String, String> uploadImageAndGetDetails(MultipartFile file);
    void deleteImage(String publicId);
}
