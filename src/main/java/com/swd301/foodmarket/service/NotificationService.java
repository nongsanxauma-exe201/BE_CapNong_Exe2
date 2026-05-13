package com.swd301.foodmarket.service;

import com.swd301.foodmarket.dto.request.NotificationRequest;
import com.swd301.foodmarket.dto.response.NotificationResponse;

import java.util.List;

public interface NotificationService {
    public void createNotificationForGroups(NotificationRequest request);
    public List<NotificationResponse> getMyNotifications();
    public List<NotificationResponse> getAllNotifications();
    public void markAsRead(Integer id);
    public void deleteNotification(Integer id);
}
