package com.puredo.blog.config;

import com.puredo.blog.security.HttpCookieOAuth2AuthorizationRequestRepository;
import com.puredo.blog.security.JwtFilter;
import com.puredo.blog.security.OAuth2AuthenticationSuccessHandler;
import com.puredo.blog.security.OAuth2UserServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
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
    @Order(1)
    public SecurityFilterChain apiFilterChain(HttpSecurity http, JwtFilter jwtFilter) throws Exception {
        http
            .securityMatcher("/api/**", "/health")
            .cors(Customizer.withDefaults())
            .csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.GET,  "/health").permitAll()
                .requestMatchers(HttpMethod.GET,  "/api/posts/feed").authenticated()
                .requestMatchers(HttpMethod.GET,  "/api/posts/explore").authenticated()
                .requestMatchers(HttpMethod.GET,  "/api/posts/mine").authenticated()
                .requestMatchers(HttpMethod.GET,  "/api/posts/**").permitAll()
                .requestMatchers(HttpMethod.GET,  "/api/users/{username}/isFollowing").authenticated()
                .requestMatchers(HttpMethod.GET,  "/api/users/preferences").authenticated()
                .requestMatchers(HttpMethod.GET,  "/api/users/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/users/createUser").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/events/**").permitAll()
                .requestMatchers(HttpMethod.GET,  "/api/events/summary").authenticated()
                .requestMatchers(HttpMethod.GET,  "/api/events/**").permitAll()
                .requestMatchers(HttpMethod.PUT,    "/api/posts/subjects/**").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/api/posts/subjects/**").authenticated()
                .requestMatchers(HttpMethod.GET,  "/api/comments/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/comments/**").authenticated()
                .requestMatchers(HttpMethod.PUT,  "/api/comments/**").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/api/comments/**").authenticated()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
            .exceptionHandling(e -> e
                .authenticationEntryPoint((req, res, ex) -> {
                    res.setStatus(401);
                    res.setContentType("application/json");
                    res.getWriter().write("{\"error\":\"Unauthorized\"}");
                })
            );

        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain oauthFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(Customizer.withDefaults())
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth.anyRequest().authenticated());

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