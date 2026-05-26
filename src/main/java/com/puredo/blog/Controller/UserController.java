package com.puredo.blog.Controller;

import com.puredo.blog.DTO.UserDTO;
import com.puredo.blog.Entity.User;
import com.puredo.blog.Service.User.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "http://localhost:3000")
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/createUser")
    public ResponseEntity<UserDTO.Response.UsuarioPublico> createUser(@RequestBody UserDTO.Request.Create request) {
        return userService.registerUser(request)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.badRequest().build());
    }

    @GetMapping("/listUsers")
    public ResponseEntity<List<UserDTO.Response.UsuarioPublico>> getAllUsers() {
        List<UserDTO.Response.UsuarioPublico> responses = userService.getAllUsers().stream()
            .map(u -> new UserDTO.Response.UsuarioPublico(u.getId(), u.getUsername(), null))
            .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @PutMapping("/updateUser")
    public ResponseEntity<UserDTO.Response.UsuarioPrivado> updateUser(
        @RequestParam Long id,
        @RequestBody UserDTO.Request.Update request
    ) {
        return userService.updateUser(id, request)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/deleteUser")
    public ResponseEntity<Void> deleteUser(@RequestParam Long id) {
        if (userService.getUserById(id).isEmpty()) return ResponseEntity.notFound().build();
        userService.deleteUserById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/getUserByUsername")
    public ResponseEntity<UserDTO.Response.UsuarioPublico> getUserByUsername(@RequestParam String username) {
        return userService.findByUserName(username)
            .map(u -> ResponseEntity.ok(new UserDTO.Response.UsuarioPublico(u.getId(), u.getUsername(), null)))
            .orElse(ResponseEntity.notFound().build());
    }
}
