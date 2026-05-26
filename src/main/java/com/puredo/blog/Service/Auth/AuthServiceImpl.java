package com.puredo.blog.Service.Auth;

import com.puredo.blog.DTO.AuthDTO;
import com.puredo.blog.Entity.User;
import com.puredo.blog.Service.User.UserService;
import com.puredo.blog.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public AuthServiceImpl(UserService userService, JwtUtil jwtUtil, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public Optional<AuthDTO.Response.Token> login(AuthDTO.Request.Login request) {
        Optional<User> user = userService.findByUserName(request.getUsername());
        if (user.isEmpty() || !passwordEncoder.matches(request.getPassword(), user.get().getPassword())) {
            return Optional.empty();
        }
        String token = jwtUtil.generateToken(user.get().getUsername());
        return Optional.of(new AuthDTO.Response.Token(token, user.get().getUsername()));
    }
}
