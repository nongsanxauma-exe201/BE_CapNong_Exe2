package com.swd301.foodmarket.repository;

import com.swd301.foodmarket.entity.ChatMessage;
import com.swd301.foodmarket.entity.Conversation;
import com.swd301.foodmarket.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Integer> {

    /**
     * Lịch sử chat trong 1 conversation, sắp xếp theo thời gian tăng dần
     */
    List<ChatMessage> findByConversationOrderBySentAtAsc(Conversation conversation);

    /**
     * Đếm số tin chưa đọc (gửi bởi người khác, chưa đọc)
     */
    @Query("SELECT COUNT(m) FROM ChatMessage m WHERE m.conversation = :conv AND m.sender != :me AND m.isRead = false")
    int countUnread(@Param("conv") Conversation conv, @Param("me") User me);

    /**
     * Đánh dấu tất cả tin nhắn từ người kia trong conversation là đã đọc
     */
    @Modifying
    @Query("UPDATE ChatMessage m SET m.isRead = true WHERE m.conversation = :conv AND m.sender != :me AND m.isRead = false")
    void markAllAsRead(@Param("conv") Conversation conv, @Param("me") User me);
}