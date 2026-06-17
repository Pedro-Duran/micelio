package com.puredo.blog.Service.Notification;

import com.puredo.blog.DTO.NotificationDTO;
import com.puredo.blog.Entity.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NotificationService {
    void notify(String recipientUsername, NotificationType type, String actorUsername, String actorAvatarUrl, Long postId, String postTitle);
    Page<NotificationDTO.Response.NotificationItem> getNotifications(String username, Pageable pageable);
    long getUnreadCount(String username);
    void markAllRead(String username);
    boolean markRead(Long id, String username);
}
