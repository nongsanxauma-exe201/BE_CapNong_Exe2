package com.swd301.foodmarket.repository;

import com.swd301.foodmarket.entity.User;
import com.swd301.foodmarket.entity.UserNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserNotificationRepository extends JpaRepository<UserNotification, Integer> {
    List<UserNotification> findByUserAndIsDeletedFalseOrderByNotificationCreateAtDesc(User user);
    Optional<UserNotification> findByIdAndUser(Integer id, User user);
}
