package com.puredo.blog.Controller;

import com.puredo.blog.DTO.UserDTO;
import com.puredo.blog.Entity.User;
import com.puredo.blog.Service.Follow.FollowService;
import com.puredo.blog.Service.Storage.StorageService;
import com.puredo.blog.Service.User.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "http://localhost:3000")
public class UserController {

    private final UserService userService;
    private final FollowService followService;
    private final StorageService storageService;

    @Autowired
    public UserController(UserService userService, FollowService followService, StorageService storageService) {
        this.userService = userService;
        this.followService = followService;
        this.storageService = storageService;
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
            .map(u -> new UserDTO.Response.UsuarioPublico(u.getId(), u.getUsername(), null, u.getAvatarUrl()))
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
            .map(u -> ResponseEntity.ok(new UserDTO.Response.UsuarioPublico(u.getId(), u.getUsername(), null, u.getAvatarUrl())))
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/search")
    public ResponseEntity<List<UserDTO.Response.UsuarioPublico>> searchUsers(
        @RequestParam(required = false) String username
    ) {
        return ResponseEntity.ok(userService.searchUsers(username));
    }

    @PostMapping("/{id}/avatar")
    public ResponseEntity<?> uploadAvatar(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        try {
            String url = storageService.uploadAvatar(file);
            return userService.updateAvatar(id, url)
                .map(avatarUrl -> ResponseEntity.ok((Object) new UserDTO.Upload.AvatarResponse(avatarUrl)))
                .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // --- Follow ---

    @PostMapping("/{username}/follow")
    public ResponseEntity<Void> follow(@PathVariable String username, Authentication authentication) {
        try {
            followService.follow(authentication.getName(), username);
            return ResponseEntity.status(HttpStatus.CREATED).build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{username}/follow")
    public ResponseEntity<Void> unfollow(@PathVariable String username, Authentication authentication) {
        followService.unfollow(authentication.getName(), username);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{username}/following")
    public ResponseEntity<List<UserDTO.Response.UsuarioPublico>> getFollowing(@PathVariable String username) {
        return ResponseEntity.ok(followService.getFollowing(username));
    }

    @GetMapping("/{username}/followers")
    public ResponseEntity<List<UserDTO.Response.UsuarioPublico>> getFollowers(@PathVariable String username) {
        return ResponseEntity.ok(followService.getFollowers(username));
    }

    @GetMapping("/{username}/isFollowing")
    public ResponseEntity<Boolean> isFollowing(@PathVariable String username, Authentication authentication) {
        return ResponseEntity.ok(followService.isFollowing(authentication.getName(), username));
    }
}
