package com.puredo.blog.DTO;

import lombok.Value;

public enum NotificationDTO {;

    public enum Response {;

        @Value
        public static class NotificationItem {
            Long id;
            String type;
            String actorUsername;
            String actorAvatarUrl;
            Long postId;
            String postTitle;
            String createdAt;
            String readAt;
        }
    }
}
