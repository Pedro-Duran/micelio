package com.puredo.blog.DTO;

import com.puredo.blog.Entity.EventType;
import lombok.Data;
import lombok.Value;

import java.util.List;
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
            String subject;
            String coverImageUrl;
            String username;
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
            String username;
            String subject;
            String coverImageUrl;
        }

        @Value
        public static class Summary {
            Long postId;
            String title;
            long viewCount;
            double avgDuration;
            long nodeClickCount;
            long commentCount;
            long likeCount;
            long shareCount;
        }

        @Value
        public static class ReferrerSummary {
            String username;
            long totalReferrals;
            Map<String, Long> byPlatform;
        }

        @Value
        public static class SubscriptionAnalytics {
            List<TopSubscriber> topSubscribers;
            List<TopAuthor> topAuthors;
        }

        @Value
        public static class TopSubscriber {
            String username;
            long count;
        }

        @Value
        public static class TopAuthor {
            String authorUsername;
            long subscriptionCount;
        }

        @Value
        public static class CoverConversionEntry {
            Long postId;
            String postTitle;
            String subject;
            String coverImageUrl;
            long clickCount;
            long viewCount;
        }

        @Value
        public static class ShareLeaderboardEntry {
            String username;
            long shareCount;
        }
    }
}
