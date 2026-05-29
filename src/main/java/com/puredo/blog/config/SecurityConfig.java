package com.puredo.blog.config;

import com.puredo.blog.security.HttpCookieOAuth2AuthorizationRequestRepository;
import com.puredo.blog.security.JwtFilter;
import com.puredo.blog.security.OAuth2AuthenticationSuccessHandler;
import com.puredo.blog.security.OAuth2UserServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    @Autowired
    private OAuth2UserServiceImpl oAuth2UserService;

    @Autowired
    private OAuth2AuthenticationSuccessHandler oAuth2SuccessHandler;

    @Autowired
    private HttpCookieOAuth2AuthorizationRequestRepository cookieAuthorizationRequestRepository;

    @Autowired(required = false)
    private ClientRegistrationRepository clientRegistrationRepository;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtFilter jwtFilter) throws Exception {
        http
            .cors(Customizer.withDefaults())
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.GET,  "/api/posts/feed").authenticated()
                .requestMatchers(HttpMethod.GET,  "/api/posts/explore").authenticated()
                .requestMatchers(HttpMethod.GET,  "/api/posts/mine").authenticated()
                .requestMatchers(HttpMethod.GET,  "/api/posts/**").permitAll()
                .requestMatchers(HttpMethod.GET,  "/api/users/{username}/isFollowing").authenticated()
                .requestMatchers(HttpMethod.GET,  "/api/users/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/users/createUser").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/events/**").permitAll()
                .requestMatchers(HttpMethod.GET,  "/api/events/summary").authenticated()
                .requestMatchers(HttpMethod.GET,  "/api/events/**").permitAll()
                .requestMatchers(HttpMethod.GET,  "/api/comments/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/comments/**").authenticated()
                .requestMatchers(HttpMethod.PUT, "/api/comments/**").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/api/comments/**").authenticated()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        if (clientRegistrationRepository != null) {
            http.oauth2Login(oauth2 -> oauth2
                .authorizationEndpoint(endpoint -> endpoint
                    .authorizationRequestRepository(cookieAuthorizationRequestRepository)
                )
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(oAuth2UserService)
                )
                .successHandler(oAuth2SuccessHandler)
            );
        }

        return http.build();
    }
}
