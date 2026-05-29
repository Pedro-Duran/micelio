package com.puredo.blog.Service.Auth;

import com.puredo.blog.DTO.AuthDTO;
import com.puredo.blog.Entity.PasswordResetToken;
import com.puredo.blog.Entity.User;
import com.puredo.blog.Repository.PasswordResetToken.PasswordResetTokenRepository;
import com.puredo.blog.Repository.User.UserRepository;
import com.puredo.blog.Service.Email.EmailService;
import com.puredo.blog.Service.User.UserService;
import com.puredo.blog.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserService userService;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final EmailService emailService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Autowired
    public AuthServiceImpl(UserService userService,
                           UserRepository userRepository,
                           JwtUtil jwtUtil,
                           PasswordEncoder passwordEncoder,
                           PasswordResetTokenRepository resetTokenRepository,
                           EmailService emailService) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
        this.resetTokenRepository = resetTokenRepository;
        this.emailService = emailService;
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

    @Override
    @Transactional
    public void forgotPassword(AuthDTO.Request.ForgotPassword request) {
        Optional<User> userOpt = userRepository.findByEmail(request.getEmail());
        if (userOpt.isEmpty()) {
            // Retorna silenciosamente para não revelar se o email existe
            return;
        }
        User user = userOpt.get();
        resetTokenRepository.deleteByUser(user);

        String token = UUID.randomUUID().toString();
        resetTokenRepository.save(new PasswordResetToken(token, user, LocalDateTime.now().plusHours(1)));

        String resetLink = frontendUrl + "/reset-password?token=" + token;
        emailService.sendPasswordReset(user.getEmail(), resetLink);
    }

    @Override
    @Transactional
    public boolean resetPassword(AuthDTO.Request.ResetPassword request) {
        Optional<PasswordResetToken> tokenOpt = resetTokenRepository.findByToken(request.getToken());
        if (tokenOpt.isEmpty() || tokenOpt.get().isExpired()) {
            return false;
        }
        PasswordResetToken resetToken = tokenOpt.get();
        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        resetTokenRepository.delete(resetToken);
        return true;
    }
}
