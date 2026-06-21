package com.kartik.terminal.dto;

import jakarta.validation.constraints.*;
import lombok.*;

// =========================================
// AUTH REQUEST DTOs
// =========================================

public class AuthDTOs {

    // --- Register Request ---
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RegisterRequest {

        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 20, message = "Username must be 3-20 characters")
        @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Username can only contain letters, numbers, underscore")
        private String username;

        @NotBlank(message = "Email is required")
        @Email(message = "Please provide a valid email")
        private String email;

        @NotBlank(message = "Password is required")
        @Size(min = 6, max = 40, message = "Password must be 6-40 characters")
        private String password;

        @Size(max = 100, message = "Full name too long")
        private String fullName;
    }

    // --- Login Request ---
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginRequest {

        @NotBlank(message = "Username or email is required")
        private String usernameOrEmail;

        @NotBlank(message = "Password is required")
        private String password;
    }

    // --- Auth Response (JWT) ---
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AuthResponse {
        private String token;
        private String tokenType;
        private String username;
        private String email;
        private String fullName;
        private String role;
        private Long userId;
        private String message;
        private boolean success;

        public static AuthResponse success(String token, String username, String email,
                                           String fullName, String role, Long userId) {
            return AuthResponse.builder()
                    .token(token)
                    .tokenType("Bearer")
                    .username(username)
                    .email(email)
                    .fullName(fullName)
                    .role(role)
                    .userId(userId)
                    .success(true)
                    .message("Authentication successful")
                    .build();
        }
    }

    // --- Change Password ---
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChangePasswordRequest {
        @NotBlank
        private String currentPassword;

        @NotBlank
        @Size(min = 6, max = 40)
        private String newPassword;
    }

    // --- Update Profile ---
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateProfileRequest {
        @Size(max = 100)
        private String fullName;

        @Email
        private String email;

        private String avatarUrl;
    }
}
