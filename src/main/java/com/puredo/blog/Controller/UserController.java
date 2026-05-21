package com.puredo.blog.Controller;



import com.puredo.blog.DTO.PostDTO;
import com.puredo.blog.DTO.UserDTO;

import com.puredo.blog.Entity.User;
import com.puredo.blog.User.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "http://localhost:3000")
public class UserController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserController(UserService userService, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    // Endpoint para criar um novo usuário
    @PostMapping("/createUser")
    public ResponseEntity<UserDTO.Response.UsuarioPublico> createUser(@RequestBody UserDTO.Request.Create request) {
        // Verificar se o username já existe

        Optional<User> existingUser = userService.findByUserName(request.getUsername());
        if (existingUser.isPresent()) {
            return ResponseEntity.badRequest().body(null); // Retorna erro se o usuário já existir
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        User createdUser = userService.createUser(user);

        UserDTO.Response.UsuarioPublico response = new UserDTO.Response.UsuarioPublico(
                createdUser.getId(),
                createdUser.getUsername(),
                null // Lista de posts pode ser adicionada se necessário
        );

        return ResponseEntity.ok(response);
    }

    // Endpoint para listar todos os usuários
    @GetMapping("/listUsers")
    public ResponseEntity<List<UserDTO.Response.UsuarioPublico>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        System.out.println(users);

        List<UserDTO.Response.UsuarioPublico> responses = users.stream()
                .map(user -> new UserDTO.Response.UsuarioPublico(
                        user.getId(),
                        user.getUsername(),
                        null // Lista de posts pode ser convertida se necessário
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    // Endpoint para atualizar um usuário
    @PutMapping("/updateUser")
    public ResponseEntity<UserDTO.Response.UsuarioPrivado> updateUser(@RequestParam Long id, @RequestBody UserDTO.Request.Update request) {

        Optional<User> existingUser = userService.getUserById(id);
        if (existingUser.isEmpty()) {
            return ResponseEntity.notFound().build(); // Retorna 404 se o usuário não for encontrado
        }

        User user = existingUser.get();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        User updatedUser = userService.updateUser(user);

        UserDTO.Response.UsuarioPrivado response = new UserDTO.Response.UsuarioPrivado(
                updatedUser.getId(),
                updatedUser.getUsername(),
                null // Lista de posts pode ser convertida se necessário
        );

        return ResponseEntity.ok(response);
    }

    // Endpoint para deletar um usuário
    @DeleteMapping("/deleteUser")
    public ResponseEntity<Void> deleteUser(@RequestParam Long id) {
        Optional<User> existingUser = userService.getUserById(id);
        if (existingUser.isEmpty()) {
            return ResponseEntity.notFound().build(); // Retorna 404 se o usuário não for encontrado
        }

        userService.deleteUserById(id);
        return ResponseEntity.noContent().build(); // Retorna 204 (No Content) após exclusão
    }

    // Endpoint para buscar um usuário pelo username
    @GetMapping("/getUserByUsername")
    public ResponseEntity<UserDTO.Response.UsuarioPublico> getUserByUsername(@RequestParam String username) {
        Optional<User> user = userService.findByUserName(username);
        if (user.isEmpty()) {
            return ResponseEntity.notFound().build(); // Retorna 404 se o usuário não for encontrado
        }

        UserDTO.Response.UsuarioPublico response = new UserDTO.Response.UsuarioPublico(
                user.get().getId(),
                user.get().getUsername(),
                null // Lista de posts pode ser convertida se necessário
        );

        return ResponseEntity.ok(response);
    }
}
