package com.puredo.blog.DTO;

import com.puredo.blog.Entity.EventType;
import lombok.Data;
import lombok.Value;

public enum EventDTO {;

    public enum Request {;

        @Data
        @Value
        public static class Register {
            Long postId;
            EventType eventType;
            String sessionId;
            Long duration;
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
        }

        @Value
        public static class Summary {
            Long postId;
            String title;
            long viewCount;
            double avgDuration;
            long nodeClickCount;
        }
    }
}
