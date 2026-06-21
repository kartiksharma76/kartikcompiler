package com.kartik.terminal.controller;

import com.kartik.terminal.dto.AuthDTOs.*;
import com.kartik.terminal.entity.User;
import com.kartik.terminal.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    // ========== REGISTER ==========
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            AuthResponse response = authService.register(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ========== LOGIN ==========
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            AuthResponse response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ========== GET CURRENT USER INFO ==========
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getCurrentUser() {
        try {
            User user = authService.getCurrentUser();
            return ResponseEntity.ok(Map.of(
                    "id", user.getId(),
                    "username", user.getUsername(),
                    "email", user.getEmail(),
                    "fullName", user.getFullName() != null ? user.getFullName() : "",
                    "role", user.getRole().name(),
                    "totalPoints", user.getTotalPoints(),
                    "totalExecutions", user.getTotalExecutions(),
                    "favoriteLanguage", user.getFavoriteLanguage(),
                    "memberSince", user.getCreatedAt().toString(),
                    "avatarUrl", user.getAvatarUrl() != null ? user.getAvatarUrl() : ""
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "Not authenticated"));
        }
    }

    // ========== CHANGE PASSWORD ==========
    @PostMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        try {
            String message = authService.changePassword(request);
            return ResponseEntity.ok(Map.of("success", true, "message", message));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ========== UPDATE PROFILE ==========
    @PutMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        try {
            User updated = authService.updateProfile(request);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Profile updated successfully!",
                    "username", updated.getUsername(),
                    "email", updated.getEmail(),
                    "fullName", updated.getFullName() != null ? updated.getFullName() : "",
                    "avatarUrl", updated.getAvatarUrl() != null ? updated.getAvatarUrl() : ""
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ========== CHECK USERNAME AVAILABILITY ==========
    @GetMapping("/check-username/{username}")
    public ResponseEntity<?> checkUsername(@PathVariable String username) {
        boolean exists = authService.getCurrentUser() != null;
        // This is a public endpoint to check username availability during registration
        return ResponseEntity.ok(Map.of(
                "username", username,
                "available", !authService.isUsernameTaken(username)
        ));
    }

    // ========== LOGOUT (Client-side - just for logging) ==========
    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> logout() {
        // JWT is stateless - just log it
        try {
            User user = authService.getCurrentUser();
            log.info("User logged out: {}", user.getUsername());
        } catch (Exception ignored) {}
        return ResponseEntity.ok(Map.of("success", true, "message", "Logged out successfully"));
    }
}
