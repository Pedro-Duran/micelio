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

        @Data
        @Value
        public static class ForgotPassword {
            String email;
        }

        @Data
        @Value
        public static class ResetPassword {
            String token;
            String newPassword;
        }

        @Data
        @Value
        public static class RefreshToken {
            String refreshToken;
        }
    }

    public enum Response {;

        @Value
        public static class Token {
            String accessToken;
            String refreshToken;
            long expiresIn;
            String username;
        }

        @Value
        public static class Refreshed {
            String accessToken;
            String refreshToken;
            long expiresIn;
        }
    }
}
