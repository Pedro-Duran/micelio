package com.puredo.blog.Controller;

import com.puredo.blog.DTO.AuthDTO;
import com.puredo.blog.Service.Auth.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:3000")
public class AuthController {

    private final AuthService authService;

    @Autowired
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthDTO.Response.Token> login(@RequestBody AuthDTO.Request.Login request) {
        return authService.login(request)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@RequestBody AuthDTO.Request.ForgotPassword request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@RequestBody AuthDTO.Request.ResetPassword request) {
        boolean success = authService.resetPassword(request);
        return success ? ResponseEntity.ok().build() : ResponseEntity.badRequest().build();
    }
}
