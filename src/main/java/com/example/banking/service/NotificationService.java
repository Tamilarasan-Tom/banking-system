package com.example.banking.service;

import com.example.banking.entity.Notification;
import com.example.banking.entity.NotificationType;
import com.example.banking.entity.User;
import java.util.List;

public interface NotificationService {
    void createNotification(User user, String title, String message, NotificationType type);
    List<Notification> getNotificationsForUser(User user);
    void markAsRead(Long notificationId, User user);
    long getUnreadCount(User user);
}
