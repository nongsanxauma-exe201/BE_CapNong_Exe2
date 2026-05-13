package com.swd301.foodmarket.repository;

import com.swd301.foodmarket.entity.Blog;
import com.swd301.foodmarket.enums.BlogStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BlogRepository extends JpaRepository<Blog, Integer> {

    List<Blog> findAllByStatus(BlogStatus status);
    Page<Blog> findAll(Pageable pageable);
    Page<Blog> findAllByStatus(BlogStatus status, Pageable pageable);

    List<Blog> findAllByAdminId(Integer adminId);
    @Query("""
           SELECT b FROM Blog b
           WHERE YEAR(b.createAt) = :year
           AND MONTH(b.createAt) = :month
           AND DAY(b.createAt) = :day
           ORDER BY b.createAt DESC
           """)
    List<Blog> findBlogsByCreateAt(@Param("year") Integer year,
                                   @Param("month") Integer month,
                                   @Param("day") Integer day);
}