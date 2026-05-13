package com.swd301.foodmarket.service.impl;

import com.swd301.foodmarket.dto.request.BlogCreationRequest;
import com.swd301.foodmarket.dto.request.BlogUpdateRequest;
import com.swd301.foodmarket.dto.response.BlogResponse;
import com.swd301.foodmarket.dto.response.PageResponse;
import com.swd301.foodmarket.entity.Blog;
import com.swd301.foodmarket.entity.User;
import com.swd301.foodmarket.enums.BlogStatus;
import com.swd301.foodmarket.exception.AppException;
import com.swd301.foodmarket.exception.ErrorCode;
import com.swd301.foodmarket.mapper.BlogMapper;
import com.swd301.foodmarket.repository.BlogRepository;
import com.swd301.foodmarket.repository.UserRepository;
import com.swd301.foodmarket.service.BlogService;
import com.swd301.foodmarket.service.CloudinaryService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class BlogServiceImpl implements BlogService {

    BlogRepository blogRepository;
    UserRepository userRepository;
    BlogMapper blogMapper;
    CloudinaryService cloudinaryService;

    // Lấy admin đang đăng nhập từ SecurityContext
    private User getCurrentAdmin() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }

    @Override
    public BlogResponse createBlog(BlogCreationRequest request, MultipartFile picture) {
        log.info("Creating blog with title: {}", request.getTitle());

        User admin = getCurrentAdmin();

        Blog blog = blogMapper.toBlog(request);
        blog.setAdmin(admin);

        // Nếu không set status thì mặc định là DRAFT
        if (blog.getStatus() == null) {
            blog.setStatus(BlogStatus.DRAFT);
        }

        // Upload ảnh nếu có
        if (picture != null && !picture.isEmpty()) {
            String pictureCloudUrl = cloudinaryService.uploadImage(picture);
            blog.setPictureUrl(pictureCloudUrl);
        }

        Blog savedBlog = blogRepository.save(blog);
        log.info("Blog created successfully with ID: {}", savedBlog.getId());

        return blogMapper.toBlogResponse(savedBlog);
    }

    @Override
    public BlogResponse updateBlog(Integer blogId, BlogUpdateRequest request, MultipartFile picture) {
        log.info("Updating blog with ID: {}", blogId);

        Blog blog = blogRepository.findById(blogId)
                .orElseThrow(() -> new AppException(ErrorCode.BLOG_NOT_FOUND));

        // Update các field không null
        blogMapper.updateBlog(blog, request);

        // Upload ảnh mới nếu có
        if (picture != null && !picture.isEmpty()) {
            String pictureCloudUrl = cloudinaryService.uploadImage(picture);
            blog.setPictureUrl(pictureCloudUrl);
        }

        Blog updatedBlog = blogRepository.save(blog);
        log.info("Blog updated successfully: {}", blogId);

        return blogMapper.toBlogResponse(updatedBlog);
    }

    @Override
    public BlogResponse getBlogById(Integer blogId) {
        log.info("Getting blog with ID: {}", blogId);

        Blog blog = blogRepository.findById(blogId)
                .orElseThrow(() -> new AppException(ErrorCode.BLOG_NOT_FOUND));

        if (blog.getViews() == null) {
            blog.setViews(0);
        }
        blog.setViews(blog.getViews() + 1);
        blogRepository.save(blog);

        return blogMapper.toBlogResponse(blog);
    }

    @Override
    public List<BlogResponse> getAllBlogs() {
        log.info("Getting all blogs");

        return blogRepository.findAll()
                .stream()
                .map(blogMapper::toBlogResponse)
                .collect(Collectors.toList());
    }

    @Override
    public PageResponse<BlogResponse> getAllBlogsPaged(int page, int size) {
        log.info("Getting all blogs paged: page={}, size={}", page, size);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createAt").descending());
        Page<Blog> pageResult = blogRepository.findAll(pageable);
        return PageResponse.<BlogResponse>builder()
                .content(pageResult.getContent().stream().map(blogMapper::toBlogResponse).toList())
                .page(pageResult.getNumber())
                .size(pageResult.getSize())
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .first(pageResult.isFirst())
                .last(pageResult.isLast())
                .build();
    }

    @Override
    public List<BlogResponse> getPublishedBlogs() {
        log.info("Getting all published blogs");

        return blogRepository.findAllByStatus(BlogStatus.PUBLISHED)
                .stream()
                .map(blogMapper::toBlogResponse)
                .collect(Collectors.toList());
    }

    @Override
    public PageResponse<BlogResponse> getPublishedBlogsPaged(int page, int size) {
        log.info("Getting published blogs paged: page={}, size={}", page, size);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createAt").descending());
        Page<Blog> pageResult = blogRepository.findAllByStatus(BlogStatus.PUBLISHED, pageable);
        return PageResponse.<BlogResponse>builder()
                .content(pageResult.getContent().stream().map(blogMapper::toBlogResponse).toList())
                .page(pageResult.getNumber())
                .size(pageResult.getSize())
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .first(pageResult.isFirst())
                .last(pageResult.isLast())
                .build();
    }

    @Override
    public void deleteBlog(Integer blogId) {
        log.info("Deleting blog with ID: {}", blogId);

        if (!blogRepository.existsById(blogId)) {
            throw new AppException(ErrorCode.BLOG_NOT_FOUND);
        }

        blogRepository.deleteById(blogId);
        log.info("Blog deleted successfully: {}", blogId);
    }

    @Override
    public List<BlogResponse> getBlogByDate(Integer year, Integer month, Integer day) {

        log.info("Getting blogs by date: {}/{}/{}", year, month, day);

        List<Blog> blogs = blogRepository.findBlogsByCreateAt(year, month, day);

        return blogs.stream()
                .map(blogMapper::toBlogResponse)
                .toList();
    }
}