package com.swd301.foodmarket.repository;

import com.swd301.foodmarket.entity.BotKnowledge;
import com.swd301.foodmarket.enums.KnowledgeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BotKnowledgeRepository extends JpaRepository<BotKnowledge, Integer> {

    // Lấy tất cả knowledge đang active theo type
    List<BotKnowledge> findByTypeAndActiveTrue(KnowledgeType type);

    // Lấy tất cả knowledge đang active
    List<BotKnowledge> findByActiveTrue();

    // Tìm kiếm theo keyword trong title và keywords
    @Query("""
        SELECT k FROM BotKnowledge k
        WHERE k.active = true
        AND (
            LOWER(k.title)    LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(k.keywords) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(k.content)  LIKE LOWER(CONCAT('%', :keyword, '%'))
        )
    """)
    List<BotKnowledge> searchByKeyword(@Param("keyword") String keyword);
}