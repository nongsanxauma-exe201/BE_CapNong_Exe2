package com.swd301.foodmarket.mapper;

import com.swd301.foodmarket.dto.response.ImageResponse;
import com.swd301.foodmarket.entity.Image;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ImageMapper {
    ImageResponse toImageResponse(Image image);
}
