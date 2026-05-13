package com.swd301.foodmarket.service;

import com.swd301.foodmarket.dto.request.BlogCreationRequest;
import com.swd301.foodmarket.dto.request.BlogUpdateRequest;
import com.swd301.foodmarket.dto.response.BlogResponse;
import com.swd301.foodmarket.dto.response.PageResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface BlogService {

    BlogResponse createBlog(BlogCreationRequest request, MultipartFile picture);

    BlogResponse updateBlog(Integer blogId, BlogUpdateRequest request, MultipartFile picture);

    BlogResponse getBlogById(Integer blogId);

    List<BlogResponse> getAllBlogs();
    PageResponse<BlogResponse> getAllBlogsPaged(int page, int size);

    List<BlogResponse> getPublishedBlogs();
    PageResponse<BlogResponse> getPublishedBlogsPaged(int page, int size);

    void deleteBlog(Integer blogId);

    List<BlogResponse> getBlogByDate(Integer year, Integer month, Integer day);
}