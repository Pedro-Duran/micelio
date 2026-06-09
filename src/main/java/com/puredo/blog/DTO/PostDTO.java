package com.puredo.blog.DTO;

import jakarta.annotation.Nullable;
import lombok.Data;
import lombok.Value;

import java.util.List;

public enum PostDTO {;

    private interface Id {
        Long getId();
    }

    private interface Title {
        String getTitle();
    }

    private interface Content {
        String getContent();
    }

    private interface Author {
        UserDTO.Response.UsuarioPublico getAuthor();
    }

    private interface CreatedAt {
        String getCreatedAt();
    }

    @Nullable
    private interface Links {
        List<Long> getLinks();
    }

    private interface Subjects {
        List<String> getSubjects();
    }

    public enum Request {;

        @Data
        @Value
        public static class WikilinkRequest {
            String title;
            List<String> subjects;
        }

        @Data
        @Value
        public static class Create implements Title, Content, Subjects {
            String title;
            String content;
            String authorUsername;
            List<Long> links;
            List<WikilinkRequest> wikilinks;
            List<String> subjects;
        }

        @Data
        @Value
        public static class Update implements Id, Title, Content, Links, Subjects {
            Long id;
            String title;
            String content;
            List<Long> links;
            List<WikilinkRequest> wikilinks;
            List<String> subjects;
        }
    }

    public enum Response {;

        @Value
        public static class Post implements Id, Title, Content, Author, CreatedAt {
            Long id;
            String title;
            String content;
            UserDTO.Response.UsuarioPublico author;
            String createdAt;
            List<Long> links;
            List<String> subjects;
            Boolean isStub;
            String coverImageUrl;
            long likeCount;
            boolean likedByMe;
        }

        @Value
        public static class PostSummary implements Id, Title {
            Long id;
            String title;
        }

        @Value
        public static class CoverResponse {
            String coverImageUrl;
        }

        @Value
        public static class ImageUploadResponse {
            String url;
        }
    }
}
