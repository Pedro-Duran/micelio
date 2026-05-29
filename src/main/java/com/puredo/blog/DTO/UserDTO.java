package com.puredo.blog.DTO;

import com.puredo.blog.Entity.Post;
import lombok.Data;
import lombok.Value;

import java.util.List;

public enum UserDTO {;

    // Interfaces para validação e documentação dos campos
    private interface Id {
        Long getId();
    }

    private interface Username {
        String getUsername();
    }

    private interface Password {
        String getPassword();
    }

    private interface Posts {
        List<Post> getPosts(); // Use um DTO simplificado para os posts
    }

    // DTOs para Requisições
    public enum Request {;

        @Data
        @Value
        public static class Create implements Username, Password {
            String username;
            String password;
        }

        @Value
        public static class Update implements Id, Username, Password {
            Long id;
            String username;
            String password;
        }

    }

    // DTOs para Respostas
    public enum Response {;

        @Value
        public static class UsuarioPublico implements Id, Username, Posts {
            Long id;
            String username;
            List<Post> posts;
            String avatarUrl;
        }

        @Value
        public static class UsuarioPrivado implements Id, Username, Password {
            Long id;
            String username;
            String password;
        }
    }

    public enum Upload {;

        @Value
        public static class AvatarResponse {
            String avatarUrl;
        }
    }
}
