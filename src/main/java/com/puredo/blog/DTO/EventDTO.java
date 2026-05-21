package com.puredo.blog.DTO;

import com.puredo.blog.Entity.EventType;
import lombok.Data;
import lombok.Value;

import java.util.Map;

public enum EventDTO {;

    public enum Request {;

        @Data
        @Value
        public static class Register {
            Long postId;
            EventType eventType;
            String sessionId;
            Long duration;
            String utmSource;
            String referredBy;
        }
    }

    public enum Response {;

        @Value
        public static class EventSaved {
            Long id;
            Long postId;
            EventType eventType;
            String sessionId;
            String timestamp;
            Long duration;
            String utmSource;
            String referredBy;
        }

        @Value
        public static class Summary {
            Long postId;
            String title;
            long viewCount;
            double avgDuration;
            long nodeClickCount;
        }

        @Value
        public static class ReferrerSummary {
            String username;
            long totalReferrals;
            Map<String, Long> byPlatform;
        }
    }
}
