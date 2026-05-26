package com.puredo.blog.DTO;

import lombok.Data;
import lombok.Value;

import java.util.List;

public enum CommentDTO {;

    public enum Request {;

        @Data
        @Value
        public static class Create {
            Long postId;
            String content;
        }
    }

    public enum Response {;

        @Value
        public static class Comment {
            Long id;
            String content;
            String authorUsername;
            String createdAt;
            List<Comment> replies;
        }
    }
}
