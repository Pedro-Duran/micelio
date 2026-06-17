package com.puredo.blog.Service.Notification;

import com.puredo.blog.DTO.NotificationDTO;
import com.puredo.blog.Entity.Notification;
import com.puredo.blog.Entity.NotificationType;
import com.puredo.blog.Repository.Notification.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;

    @Autowired
    public NotificationServiceImpl(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Override
    @Transactional
    public void notify(String recipientUsername, NotificationType type, String actorUsername, String actorAvatarUrl, Long postId, String postTitle) {
        if (recipientUsername.equals(actorUsername)) return;

        Notification notification = new Notification();
        notification.setRecipientUsername(recipientUsername);
        notification.setType(type);
        notification.setActorUsername(actorUsername);
        notification.setActorAvatarUrl(actorAvatarUrl);
        notification.setPostId(postId);
        notification.setPostTitle(postTitle);
        notificationRepository.save(notification);
    }

    @Override
    public Page<NotificationDTO.Response.NotificationItem> getNotifications(String username, Pageable pageable) {
        return notificationRepository
                .findByRecipientUsernameOrderByCreatedAtDesc(username, pageable)
                .map(this::toDTO);
    }

    @Override
    public long getUnreadCount(String username) {
        return notificationRepository.countByRecipientUsernameAndReadAtIsNull(username);
    }

    @Override
    @Transactional
    public void markAllRead(String username) {
        notificationRepository.markAllRead(username, LocalDateTime.now());
    }

    @Override
    @Transactional
    public boolean markRead(Long id, String username) {
        return notificationRepository.findById(id)
                .filter(n -> n.getRecipientUsername().equals(username))
                .map(n -> {
                    if (n.getReadAt() == null) {
                        n.setReadAt(LocalDateTime.now());
                        notificationRepository.save(n);
                    }
                    return true;
                })
                .orElse(false);
    }

    private NotificationDTO.Response.NotificationItem toDTO(Notification n) {
        return new NotificationDTO.Response.NotificationItem(
                n.getId(),
                n.getType().name(),
                n.getActorUsername(),
                n.getActorAvatarUrl(),
                n.getPostId(),
                n.getPostTitle(),
                n.getCreatedAt().toString(),
                n.getReadAt() != null ? n.getReadAt().toString() : null
        );
    }
}
