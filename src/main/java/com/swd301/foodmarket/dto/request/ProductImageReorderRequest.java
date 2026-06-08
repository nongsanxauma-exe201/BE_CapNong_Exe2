package com.swd301.foodmarket.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductImageReorderRequest {

    List<ImageOrderItem> images;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ImageOrderItem {
        Long id;
        Integer displayOrder;
    }
}
