package com.puredo.blog.Repository.Notification;

import com.puredo.blog.Entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByRecipientUsernameOrderByCreatedAtDesc(String recipientUsername, Pageable pageable);

    long countByRecipientUsernameAndReadAtIsNull(String recipientUsername);

    @Modifying
    @Query("UPDATE Notification n SET n.readAt = :now WHERE n.recipientUsername = :username AND n.readAt IS NULL")
    void markAllRead(@Param("username") String username, @Param("now") LocalDateTime now);
}
