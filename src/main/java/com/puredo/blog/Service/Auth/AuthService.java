package com.puredo.blog.Service.Auth;

import com.puredo.blog.DTO.AuthDTO;
import com.puredo.blog.Entity.User;

import java.util.Optional;

public interface AuthService {
    Optional<AuthDTO.Response.Token> login(AuthDTO.Request.Login request);
    void forgotPassword(AuthDTO.Request.ForgotPassword request);
    boolean resetPassword(AuthDTO.Request.ResetPassword request);
    AuthDTO.Response.Token issueTokens(User user);
    Optional<AuthDTO.Response.Refreshed> refresh(String refreshToken);
    void logout(String refreshToken);
}
