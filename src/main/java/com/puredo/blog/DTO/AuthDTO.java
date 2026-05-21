package com.puredo.blog.DTO;

import lombok.Data;
import lombok.Value;

public enum AuthDTO {;

    public enum Request {;

        @Data
        @Value
        public static class Login {
            String username;
            String password;
        }
    }

    public enum Response {;

        @Value
        public static class Token {
            String token;
            String username;
        }
    }
}
