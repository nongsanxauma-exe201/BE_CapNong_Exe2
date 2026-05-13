package com.swd301.foodmarket.repository;

import com.swd301.foodmarket.entity.Conversation;
import com.swd301.foodmarket.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Integer> {

    Optional<Conversation> findByRoomKey(String roomKey);

    /**
     * Lấy tất cả conversation của 1 user (làm inbox / danh sách chat)
     * Sắp xếp theo tin nhắn mới nhất lên đầu
     */
    @Query("SELECT c FROM Conversation c WHERE c.user1 = :user OR c.user2 = :user ORDER BY c.lastMessageAt DESC")
    List<Conversation> findAllByUser(@Param("user") User user);
}