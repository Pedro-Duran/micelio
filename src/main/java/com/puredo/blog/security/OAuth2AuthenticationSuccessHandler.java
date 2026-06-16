package com.puredo.blog.security;

import com.puredo.blog.DTO.AuthDTO;
import com.puredo.blog.Entity.User;
import com.puredo.blog.Repository.User.UserRepository;
import com.puredo.blog.Service.Auth.AuthService;
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

    private final AuthService authService;
    private final UserRepository userRepository;
    private final HttpCookieOAuth2AuthorizationRequestRepository cookieRepository;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Autowired
    public OAuth2AuthenticationSuccessHandler(AuthService authService,
                                              UserRepository userRepository,
                                              HttpCookieOAuth2AuthorizationRequestRepository cookieRepository) {
        this.authService = authService;
        this.userRepository = userRepository;
        this.cookieRepository = cookieRepository;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Usuário OAuth2 não encontrado após autenticação"));

        cookieRepository.removeAuthorizationRequest(request, response);

        AuthDTO.Response.Token tokens = authService.issueTokens(user);
        response.sendRedirect(frontendUrl + "/oauth2/callback?token=" + tokens.getAccessToken()
                + "&refreshToken=" + tokens.getRefreshToken());
    }
}
