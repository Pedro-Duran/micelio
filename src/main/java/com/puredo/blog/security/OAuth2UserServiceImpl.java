package com.puredo.blog.security;

import com.puredo.blog.Entity.User;
import com.puredo.blog.Repository.User.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
public class OAuth2UserServiceImpl extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Autowired
    public OAuth2UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest request) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(request);

        String googleId = oAuth2User.getAttribute("sub");
        String email    = oAuth2User.getAttribute("email");
        String name     = oAuth2User.getAttribute("name");
        String picture  = oAuth2User.getAttribute("picture");

        // Usuário já vinculado ao Google
        userRepository.findByGoogleId(googleId).ifPresentOrElse(
            user -> {
                if (user.getAvatarUrl() == null && picture != null) {
                    user.setAvatarUrl(picture);
                    userRepository.save(user);
                }
            },
            () -> userRepository.findByEmail(email).ifPresentOrElse(
                // Conta local com mesmo email — vincula o Google ID
                user -> {
                    user.setGoogleId(googleId);
                    if (user.getAvatarUrl() == null) user.setAvatarUrl(picture);
                    userRepository.save(user);
                },
                // Usuário novo — cria conta
                () -> {
                    User user = new User();
                    user.setGoogleId(googleId);
                    user.setEmail(email);
                    user.setUsername(generateUsername(name, email));
                    user.setAvatarUrl(picture);
                    userRepository.save(user);
                }
            )
        );

        return oAuth2User;
    }

    private String generateUsername(String name, String email) {
        String base = (name != null ? name : email.split("@")[0])
                .toLowerCase()
                .replaceAll("[^a-z0-9_]", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");

        String username = base;
        int suffix = 1;
        while (userRepository.findByUsername(username).isPresent()) {
            username = base + suffix++;
        }
        return username;
    }
}
