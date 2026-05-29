package com.puredo.blog.Service.User;

import com.puredo.blog.DTO.UserDTO;
import com.puredo.blog.Entity.User;
import com.puredo.blog.Repository.User.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public Optional<UserDTO.Response.UsuarioPublico> registerUser(UserDTO.Request.Create request) {
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            return Optional.empty();
        }
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            return Optional.empty();
        }
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            return Optional.empty();
        }
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        User saved = userRepository.save(user);
        return Optional.of(new UserDTO.Response.UsuarioPublico(saved.getId(), saved.getUsername(), null, saved.getAvatarUrl()));
    }

    @Override
    public Optional<UserDTO.Response.UsuarioPrivado> updateUser(Long id, UserDTO.Request.Update request) {
        return userRepository.findById(id).map(user -> {
            user.setUsername(request.getUsername());
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            User saved = userRepository.save(user);
            return new UserDTO.Response.UsuarioPrivado(saved.getId(), saved.getUsername(), null);
        });
    }

    @Override
    public Optional<String> updateAvatar(Long userId, String avatarUrl) {
        return userRepository.findById(userId).map(user -> {
            user.setAvatarUrl(avatarUrl);
            userRepository.save(user);
            return avatarUrl;
        });
    }

    @Override
    public Optional<String> removeAvatar(Long userId) {
        return userRepository.findById(userId).map(user -> {
            String existing = user.getAvatarUrl();
            user.setAvatarUrl(null);
            userRepository.save(user);
            return existing;
        });
    }

    @Override
    public void deleteUserById(Long id) {
        userRepository.deleteById(id);
    }

    @Override
    public Optional<User> findByUserName(String username) {
        return userRepository.findByUsername(username);
    }

    @Override
    public List<UserDTO.Response.UsuarioPublico> searchUsers(String username) {
        List<User> users = (username == null || username.isBlank())
            ? userRepository.findAll()
            : userRepository.findByUsernameContainingIgnoreCase(username);

        return users.stream()
            .map(u -> new UserDTO.Response.UsuarioPublico(u.getId(), u.getUsername(), null, u.getAvatarUrl()))
            .collect(Collectors.toList());
    }

    @Override
    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
}
