package com.swd301.foodmarket.repository;

import com.swd301.foodmarket.entity.ChatBot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatBotRepository extends JpaRepository<ChatBot, Integer> {

    List<ChatBot> findByUser_IdOrderByCreatedAtAsc(Integer userId);

    List<ChatBot> findTop10ByUser_IdOrderByCreatedAtDesc(Integer userId);
}