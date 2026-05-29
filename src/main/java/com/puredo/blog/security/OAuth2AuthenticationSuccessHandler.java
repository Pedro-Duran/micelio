package com.puredo.blog.security;

import com.puredo.blog.Repository.User.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final HttpCookieOAuth2AuthorizationRequestRepository cookieRepository;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Autowired
    public OAuth2AuthenticationSuccessHandler(JwtUtil jwtUtil,
                                              UserRepository userRepository,
                                              HttpCookieOAuth2AuthorizationRequestRepository cookieRepository) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.cookieRepository = cookieRepository;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");

        String username = userRepository.findByEmail(email)
                .map(com.puredo.blog.Entity.User::getUsername)
                .orElseThrow(() -> new IllegalStateException("Usuário OAuth2 não encontrado após autenticação"));

        cookieRepository.removeAuthorizationRequest(request, response);

        String token = jwtUtil.generateToken(username);
        response.sendRedirect(frontendUrl + "/oauth2/callback?token=" + token);
    }
}
