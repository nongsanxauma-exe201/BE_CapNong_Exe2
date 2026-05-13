package com.swd301.foodmarket.mapper;

import com.swd301.foodmarket.dto.request.BlogCreationRequest;
import com.swd301.foodmarket.dto.request.BlogUpdateRequest;
import com.swd301.foodmarket.dto.response.BlogResponse;
import com.swd301.foodmarket.entity.Blog;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface BlogMapper {

    Blog toBlog(BlogCreationRequest request);

    @Mapping(source = "admin.id", target = "adminId")
    @Mapping(source = "admin.fullName", target = "adminName")
    BlogResponse toBlogResponse(Blog blog);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateBlog(@MappingTarget Blog blog, BlogUpdateRequest request);
}