package com.swd301.foodmarket.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.swd301.foodmarket.service.CloudinaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CloudinaryServiceImpl implements CloudinaryService {

    private final Cloudinary cloudinary;

    @Override
    public String uploadImage(MultipartFile file) {
        try {
            Map<?, ?> result = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", "brands",
                            "resource_type", "image"
                    )
            );
            return result.get("secure_url").toString();
        } catch (IOException e) {
            throw new RuntimeException("Upload image to Cloudinary failed");
        }
    }

    @Override
    public void deleteImage(String publicId) {
        try {
            cloudinary.uploader().destroy(publicId, Map.of());
        } catch (Exception e) {
            throw new RuntimeException("Delete image failed");
        }
    }

}
