package com.swd301.foodmarket.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swd301.foodmarket.dto.request.BlogCreationRequest;
import com.swd301.foodmarket.dto.request.BlogUpdateRequest;
import com.swd301.foodmarket.dto.response.ApiResponse;
import com.swd301.foodmarket.dto.response.BlogResponse;
import com.swd301.foodmarket.dto.response.PageResponse;
import com.swd301.foodmarket.service.BlogService;
import com.swd301.foodmarket.service.CloudinaryService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/blogs")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class BlogController {
    CloudinaryService cloudService;
    BlogService blogService;
    /**
     * Create blog (Admin only) - multipart vì có upload ảnh
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<BlogResponse> createBlog(
            @RequestPart("data") String data,
            @RequestPart(value = "picture", required = false) MultipartFile picture
    ) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        BlogCreationRequest request = objectMapper.readValue(data, BlogCreationRequest.class);

        log.info("[POST] /blogs - Create blog");
        return ApiResponse.<BlogResponse>builder()
                .result(blogService.createBlog(request, picture))
                .build();
    }

    /**
     * Update blog (Admin only)
     */
    @PutMapping(value = "/{blogId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<BlogResponse> updateBlog(
            @PathVariable Integer blogId,
            @RequestPart("data") String data,
            @RequestPart(value = "picture", required = false) MultipartFile picture
    ) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        BlogUpdateRequest request = objectMapper.readValue(data, BlogUpdateRequest.class);

        log.info("[PUT] /blogs/{} - Update blog", blogId);
        return ApiResponse.<BlogResponse>builder()
                .result(blogService.updateBlog(blogId, request, picture))
                .build();
    }

    /**
     * Get blog by ID (Public)
     */
    @GetMapping("/{blogId}")
    public ApiResponse<BlogResponse> getBlogById(@PathVariable Integer blogId) {
        log.info("[GET] /blogs/{}", blogId);
        return ApiResponse.<BlogResponse>builder()
                .result(blogService.getBlogById(blogId))
                .build();
    }

    /**
     * Get all blogs (Admin only)
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<BlogResponse>> getAllBlogs() {
        log.info("[GET] /blogs - Get all blogs");
        return ApiResponse.<List<BlogResponse>>builder()
                .result(blogService.getAllBlogs())
                .build();
    }

    /**
     * Get all blogs paged (Admin only)
     */
    @GetMapping("/paged")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PageResponse<BlogResponse>> getAllBlogsPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        log.info("[GET] /blogs/paged - Get all blogs paged");
        return ApiResponse.<PageResponse<BlogResponse>>builder()
                .result(blogService.getAllBlogsPaged(page, size))
                .build();
    }

    /**
     * Get published blogs (Public - dành cho người dùng xem)
     */
    @GetMapping("/published")
    public ApiResponse<List<BlogResponse>> getPublishedBlogs() {
        log.info("[GET] /blogs/published - Get published blogs");
        return ApiResponse.<List<BlogResponse>>builder()
                .result(blogService.getPublishedBlogs())
                .build();
    }

    /**
     * Get published blogs paged (Public)
     */
    @GetMapping("/published/paged")
    public ApiResponse<PageResponse<BlogResponse>> getPublishedBlogsPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "9") int size
    ) {
        log.info("[GET] /blogs/published/paged - Get published blogs paged");
        return ApiResponse.<PageResponse<BlogResponse>>builder()
                .result(blogService.getPublishedBlogsPaged(page, size))
                .build();
    }

    /**
     * Delete blog (Admin only)
     */
    @DeleteMapping("/{blogId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deleteBlog(@PathVariable Integer blogId) {
        log.info("[DELETE] /blogs/{}", blogId);
        blogService.deleteBlog(blogId);
        return ApiResponse.<Void>builder()
                .message("Blog deleted successfully")
                .build();
    }
    /**
     * Get blog by date
     */
    @GetMapping("/blogs/date")
    public ApiResponse<List<BlogResponse>> getBlogsByDate(
            @RequestParam Integer year,
            @RequestParam Integer month,
            @RequestParam Integer day) {

        return ApiResponse.<List<BlogResponse>>builder()
                .result(blogService.getBlogByDate(year, month, day))
                .build();
    }

    @PostMapping(value = "/upload-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, String> uploadImage(@RequestParam("file") MultipartFile file) {

        if (file.isEmpty()) {
            throw new RuntimeException("File is empty");
        }

        String imageUrl = cloudService.uploadImage(file);

        return Map.of("url", imageUrl);
    }
}