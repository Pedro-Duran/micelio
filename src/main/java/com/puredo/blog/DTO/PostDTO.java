package com.puredo.blog.DTO;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
        UserDTO.Response.UsuarioPublico getAuthor(); // Objeto complexo
    }

    private interface CreatedAt {
        String getCreatedAt();
    }

    @Nullable
    private interface Links{
        List<Long> getLinks();
    }


    private interface Subject{
        String getSubject();
    }

    public enum Request {;

        @Data
        @Value
        public static class Create implements Title, Content, Subject {
            String title;
            String content;
            String authorUsername;
            List<Long> links;
            List<String> wikilinks;
            String subject;
        }


        @Data
        @Value
        public static class Update implements Id, Title, Content, Links, Subject {
            Long id;
            String title;
            String content;
            List<Long> links;
            List<String> wikilinks;
            String subject;
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
            String subject;     
            Boolean isStub;
        }

        @Value
        public static class PostSummary implements Id, Title {
            Long id;
            String title;
        }
    }
}
