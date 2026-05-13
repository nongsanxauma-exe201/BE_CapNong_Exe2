package com.swd301.foodmarket.repository;

import com.swd301.foodmarket.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Integer> {

    @Query("SELECT n FROM Notification n WHERE n.receiverType LIKE %:role% ORDER BY n.createAt DESC")
    List<Notification> findByReceiverTypeContaining(@Param("role") String role);

    List<Notification> findAllByOrderByCreateAtDesc();
}