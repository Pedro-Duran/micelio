package com.puredo.blog.Service.User;

import com.puredo.blog.DTO.UserDTO;
import com.puredo.blog.Entity.User;
import com.puredo.blog.Repository.User.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

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
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        User saved = userRepository.save(user);
        return Optional.of(new UserDTO.Response.UsuarioPublico(saved.getId(), saved.getUsername(), null));
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
    public void deleteUserById(Long id) {
        userRepository.deleteById(id);
    }

    @Override
    public Optional<User> findByUserName(String username) {
        return userRepository.findByUsername(username);
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
